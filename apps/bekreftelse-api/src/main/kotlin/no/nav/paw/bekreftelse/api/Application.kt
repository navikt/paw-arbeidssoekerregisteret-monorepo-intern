package no.nav.paw.bekreftelse.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureKafka
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureMetrics
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.plugins.configureTracing
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.routes.metricsRoutes
import no.nav.paw.bekreftelse.api.routes.swaggerRoutes
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.route.healthRoutes
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("no.nav.paw.logger.application")

    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)

    logger.info("Starter: ${applicationConfig.appId}")

    val dependencies = createDependencies(applicationConfig)

    with(serverConfig) {
        embeddedServer(Netty, port = port) {
            module(applicationConfig, dependencies)
        }.apply {
            addShutdownHook { stop(gracePeriodMillis, timeoutMillis) }
            start(wait = true)
        }
    }
}

fun Application.module(
    applicationConfig: ApplicationConfig,
    dependencies: Dependencies
) {
    configureMetrics(dependencies.prometheusMeterRegistry)
    configureHTTP()
    configureAuthentication(applicationConfig)
    configureLogging()
    configureSerialization()
    configureTracing()
    configureKafka(applicationConfig, listOf(dependencies.bekreftelseKafkaStreams))

    routing {
        healthRoutes(dependencies.healthIndicatorRepository)
        metricsRoutes(dependencies.prometheusMeterRegistry)
        swaggerRoutes()
        bekreftelseRoutes(
            dependencies.kafkaKeysClient,
            dependencies.autorisasjonService,
            dependencies.bekreftelseService
        )
    }
}

