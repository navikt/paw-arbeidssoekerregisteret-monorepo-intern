package no.nav.paw.bekreftelse.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import no.nav.paw.bekreftelse.api.context.ApplicationContext
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
import no.nav.paw.bekreftelse.api.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.health.route.healthRoutes

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.create()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    logger.info("Starter: $appName")

    with(applicationContext.serverConfig) {
        embeddedServer(Netty, port = port) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    configureMetrics(applicationContext)
    configureHTTP(applicationContext)
    configureAuthentication(applicationContext)
    configureLogging()
    configureSerialization()
    configureTracing()
    configureKafka(applicationContext)

    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext)
        swaggerRoutes()
        bekreftelseRoutes(applicationContext)
    }
}
