package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

const val CURRENT_VERSION = 1
const val HENDELSE_TOPIC = "paw.arbeidssoker-hendelseslogg-v1"
val CONSUMER_GROUP = "arbeidssoekerregisteret-backup-$CURRENT_VERSION"
const val ACTIVE_PARTITIONS_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_active_partitions"
const val RECORD_COUNTER = "paw_arbeidssoekerregisteret_hendelselogg_backup_records_written"
const val HWM_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_hwm"

fun initApplication(): Pair<Consumer<Long, Hendelse>, ApplicationContext> {
    val logger = LoggerFactory.getLogger("backup-init")
    logger.info("Initializing application...")
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    with(loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml")) {
        val ds = dataSource()
        logger.info("Connection to database($this)...")
        Database.Companion.connect(ds)
        logger.info("Migrating database...")
        migrateDatabase(ds)
    }
    logger.info("Connection to kafka...")
    val consumer = KafkaFactory(kafkaConfig).createConsumer(
        groupId = CONSUMER_GROUP,
        clientId = "client-$CONSUMER_GROUP",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = HendelseDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest"
    )
    val shutdown = AtomicBoolean(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdown.set(true)
    })
    val context = ApplicationContext(
        logger = LoggerFactory.getLogger("backup-context"),
        consumerVersion = CURRENT_VERSION,
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        shutdownCalled = shutdown
    )
    val partitions = consumer.partitionsFor(HENDELSE_TOPIC).count()
    with(context) {
        val allHwms = transaction {
            initHwm(partitions)
            getAllHwms()
        }
        allHwms.forEach { hwm ->
            meterRegistry.gauge(HWM_GAUGE, listOf(Tag.of("partition", hwm.partition.toString())), this) { so ->
                transaction {
                    with(so) {
                        getHwm(hwm.partition)?.toDouble() ?: -1.0
                    }
                }
            }
        }
    }
    logger.info("Application initialized")
    return Pair(consumer, context)
}
