package no.nav.paw.kafkakeymaintenance

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.*
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.kafkakeymaintenance.db.DatabaseConfig
import no.nav.paw.kafkakeymaintenance.db.dataSource
import no.nav.paw.kafkakeymaintenance.db.migrateDatabase
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorTopologyConfig
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.hentAlias
import no.nav.paw.kafkakeymaintenance.perioder.consume
import no.nav.paw.kafkakeymaintenance.perioder.dbPerioder
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    val applicationContext = ApplicationContext(
        consumerVersion = PERIODE_CONSUMER_GROUP_VERSION,
        logger = LoggerFactory.getLogger("app"),
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        shutdownCalled = AtomicBoolean(false)
    )
    Runtime.getRuntime().addShutdownHook( Thread { applicationContext.shutdownCalled.set(true) })
    val healthIndicatorRepository = HealthIndicatorRepository()
    with(loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml").dataSource()) {
        migrateDatabase(this)
        Database.connect(this)
    }
    val (hwmRebalacingListener, periodeSequence) = with(KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))) {
        initPeriodeConsumer(
            periodeTopic = PERIODE_TOPIC,
            applicationContext = applicationContext
        )
    }
    val consumerLivenessHealthIndicator = healthIndicatorRepository.addLivenessIndicator(
        LivenessHealthIndicator(HealthStatus.UNHEALTHY)
    )
    val consumerReadinessHealthIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
    runAsync {
        consumerReadinessHealthIndicator.setHealthy()
        consumerLivenessHealthIndicator.setHealthy()
        periodeSequence.consume(txContext(applicationContext))
    }.handle { _, throwable ->
        throwable?.also { applicationContext.logger.error("Consumer task failed", throwable) }
        applicationContext.shutdownCalled.set(true)
        consumerReadinessHealthIndicator.setUnhealthy()
        consumerLivenessHealthIndicator.setUnhealthy()
    }
    initStreams(
        meterRegistry = applicationContext.meterRegistry,
        aktorTopologyConfig = loadNaisOrLocalConfiguration(AktorTopologyConfig.configFile),
        healthIndicatorRepository = healthIndicatorRepository,
        perioder = dbPerioder(applicationContext),
        hentAlias = createKafkaKeyGeneratorClient()::hentAlias
    ).start()
    applicationContext.logger.info("Applikasjonen er startet, consumer: {}", hwmRebalacingListener.currentlyAssignedPartitions)
    initKtor(
        healthIndicatorRepository = healthIndicatorRepository,
        prometheusMeterRegistry = applicationContext.meterRegistry
    ).start(wait = true)
    applicationContext.shutdownCalled.set(true)
    applicationContext.logger.info("Applikasjonen er stoppet")
}
