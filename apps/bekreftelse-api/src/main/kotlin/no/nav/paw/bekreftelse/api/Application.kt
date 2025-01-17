package no.nav.paw.bekreftelse.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureDatabase
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureKafka
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureMetrics
import no.nav.paw.bekreftelse.api.plugins.configureRouting
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.plugins.configureTracing
import no.nav.paw.bekreftelse.api.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.create()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext.serverConfig) {
        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(factory = Netty, port = port) {
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
    configureDatabase(applicationContext)
    configureKafka(applicationContext)
    configureRouting(applicationContext)
}
