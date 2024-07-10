package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.APPLICATION_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.config.SERVER_LOGGER_NAME
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugins.configureKafka
import no.nav.paw.arbeidssoekerregisteret.plugins.configureMetrics
import no.nav.paw.arbeidssoekerregisteret.plugins.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugins.configureTracing
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(SERVER_LOGGER_NAME)
    val serverProperties = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationProperties = loadNaisOrLocalConfiguration<AppConfig>(APPLICATION_CONFIG_FILE_NAME)

    logger.info("Starter ${applicationProperties.appId}")

    val server = embeddedServer(
        factory = Netty,
        port = serverProperties.port,
        configure = {
            callGroupSize = serverProperties.callGroupSize
            workerGroupSize = serverProperties.workerGroupSize
            connectionGroupSize = serverProperties.connectionGroupSize
        }
    ) {
        module(applicationProperties)
    }

    server.addShutdownHook {
        server.stop(serverProperties.gracePeriodMillis, serverProperties.timeoutMillis)
        logger.info("Avslutter ${applicationProperties.appId}")
    }
    server.start(wait = true)
}

fun Application.module(properties: AppConfig) {
    val logger = LoggerFactory.getLogger(APPLICATION_LOGGER_NAME)
    val healthIndicatorService = HealthIndicatorService()
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    with(ApplicationContext(logger, properties)) {
        val kafkaStreamsMetrics = configureKafka(
            healthIndicatorService,
            meterRegistry
        )
        configureTracing()
        configureMetrics(meterRegistry, kafkaStreamsMetrics)
        configureRouting(healthIndicatorService, meterRegistry)
    }
}