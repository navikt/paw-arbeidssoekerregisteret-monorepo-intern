package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseDeserializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

const val CURRENT_VERSION = 1
const val CONSUMER_GROUP = "arbeidssoekerregisteret-bekreftelse-backup-$CURRENT_VERSION"
const val ACTIVE_PARTITIONS_GAUGE = "paw_arbeidssoekerregisteret_bekreftelse_backup_active_partitions"
const val RECORD_COUNTER = "paw_arbeidssoekerregisteret_bekreftelse_backup_records_written"
const val HWM_GAUGE = "paw_arbeidssoekerregisteret_bekreftelse_backup_hwm"

fun initApplication(): ApplicationContext {
    val logger = LoggerFactory.getLogger("bekreftelse-backup-init")
    logger.info("Initializing application...")
    val appConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)

    with(loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml")) {
        val ds = dataSource()
        logger.info("Connection to database($this)...")
        Database.connect(ds)
        logger.info("Migrating database...")
        migrateDatabase(ds)
    }

    logger.info("Connection to Kafka...")

    val hendelseConsumer = KafkaFactory(kafkaConfig).createConsumer(
        groupId = CONSUMER_GROUP,
        clientId = "bekreftelse-hendelselogg-consumer-$CONSUMER_GROUP",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = BekreftelseHendelseDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )

    val bekreftelseConsumer = KafkaFactory(kafkaConfig).createConsumer(
        groupId = CONSUMER_GROUP,
        clientId = "bekreftelse-consumer-$CONSUMER_GROUP",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )

    val paaVegneAvConsumer = KafkaFactory(kafkaConfig).createConsumer(
        groupId = CONSUMER_GROUP,
        clientId = "bekreftelse-paa-vegne-av-consumer-$CONSUMER_GROUP",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val context = ApplicationContext(
        logger = LoggerFactory.getLogger("bekreftelse-backup-context"),
        consumerVersion = CURRENT_VERSION,
        meterRegistry = meterRegistry,
        hendelseConsumer = hendelseConsumer,
        bekreftelseConsumer = bekreftelseConsumer,
        paaVegneAvConsumer = paaVegneAvConsumer,
        hendelseTopic = appConfig.hendelseTopic,
        bekreftelseTopic = appConfig.bekreftelseTopic,
        paaVegneAvTopic = appConfig.paaVegneAvTopic
    )

    val topicPartitionsMap = mapOf(
        context.hendelseTopic to hendelseConsumer.partitionsFor(context.hendelseTopic).count(),
        context.bekreftelseTopic to bekreftelseConsumer.partitionsFor(context.bekreftelseTopic).count(),
        context.paaVegneAvTopic to paaVegneAvConsumer.partitionsFor(context.paaVegneAvTopic).count()
    )

    transaction {
        val txContext = TransactionContext(context, this)
        topicPartitionsMap.forEach { (topic, partitionCount) ->
            txContext.initHwm(partitionCount, topic)
            txContext.getAllTopicHwms(topic).forEach { hwm ->
                context.meterRegistry.gauge(
                    "$HWM_GAUGE-$topic",
                    listOf(Tag.of("partition", hwm.partition.toString()), Tag.of("topic", topic)),
                    context
                ) {
                    transaction {
                        TransactionContext(context, this).getHwm(hwm.partition, topic)?.toDouble() ?: -1.0
                    }
                }
            }
        }
    }

    logger.info("Application initialized")
    return context
}
