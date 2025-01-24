package no.nav.paw.kafkakeymaintenance

import arrow.core.partially1
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.kafkakeymaintenance.db.DatabaseConfig
import no.nav.paw.kafkakeymaintenance.db.dataSource
import no.nav.paw.kafkakeymaintenance.db.migrateDatabase
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorConfig
import no.nav.paw.kafkakeymaintenance.pdlprocessor.DbReaderContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.DbReaderTask
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.hentAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.sendSync
import no.nav.paw.kafkakeymaintenance.perioder.dbPerioder
import org.apache.kafka.common.serialization.LongSerializer
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

fun main() {
    val applicationContext = ApplicationContext(
        periodeConsumerVersion = PERIODE_CONSUMER_GROUP_VERSION,
        aktorConsumerVersion = AKTOR_CONSUMER_GROUP_VERSION,
        logger = LoggerFactory.getLogger("app"),
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        shutdownCalled = AtomicBoolean(false)
    )
    Runtime.getRuntime().addShutdownHook(Thread { applicationContext.eventOccured(ShutdownSignal("Shutdown hook")) })
    val healthIndicatorRepository = HealthIndicatorRepository()
    with(loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource()) {
        migrateDatabase(this)
        Database.connect(this)
    }
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val periodeConsumer = kafkaFactory.initPeriodeConsumer(
        healthIndicatorRepository = healthIndicatorRepository,
        periodeTopic = PERIODE_TOPIC,
        applicationContext = applicationContext
    )
    val aktorConsumer = kafkaFactory.initAktorConsumer(
        healthIndicatorRepository = healthIndicatorRepository,
        aktorTopic = AKTOR_TOPIC,
        applicationContext = applicationContext,
        startDataForMergeProsessering = START_DATO_FOR_MERGE_PROSESSERING
    )
    val producer = kafkaFactory.createProducer(
        clientId = "key-maintenance-producer",
        keySerializer = LongSerializer::class,
        valueSerializer = HendelseSerializer::class
    )
    val executor = ThreadPoolExecutor(4, 4, 10L, TimeUnit.SECONDS, LinkedBlockingQueue())
    val periodeTask = periodeConsumer.run(executor)
    val aktorTask = aktorConsumer.run(executor)
    val aktorConfig = loadNaisOrLocalConfiguration<AktorConfig>(AktorConfig.configFile)
    val antallHendelsePartisjoner = producer.partitionsFor(aktorConfig.hendelseloggTopic).size
    val kafkaKeysClient = createKafkaKeyGeneratorClient()
    val dbReaderTask = DbReaderTask(
        healthIndicatorRepository = healthIndicatorRepository,
        applicationContext = applicationContext,
        dbReaderContext = DbReaderContext(
            aktorConfig = aktorConfig,
            receiver = producer::sendSync.partially1(Topic(aktorConfig.hendelseloggTopic)),
            perioder = dbPerioder(applicationContext),
            hentAlias = kafkaKeysClient::hentAlias.partially1(antallHendelsePartisjoner),
            aktorDeSerializer = kafkaFactory.kafkaAvroDeSerializer()
        )
    ).run(executor)

    applicationContext.logger.info("Applikasjonen er startet")
    initKtor(
        healthIndicatorRepository = healthIndicatorRepository,
        prometheusMeterRegistry = applicationContext.meterRegistry
    ).start(wait = false)
    awaitShutdownSignalOrError(applicationContext)
}

fun awaitShutdownSignalOrError(applicationContext: ApplicationContext) {
    while (!applicationContext.shutdownCalled.get() && !Thread.currentThread().isInterrupted) {
        applicationContext.pollMessage(Duration.ofSeconds(2)).let {
            when (it) {
                is ErrorOccurred -> {
                    applicationContext.shutdownCalled.set(true)
                    Thread.sleep(Duration.ofSeconds(2))
                    applicationContext.logger.error("Error occurred", it.throwable)
                    exitProcess(1)
                }

                is ShutdownSignal -> {
                    applicationContext.shutdownCalled.set(true)
                    applicationContext.logger.info("Shutdown signal received from ${it.source}")
                    exitProcess(0)
                }

                is Noop -> {}
            }
        }
    }
}
