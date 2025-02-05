package no.nav.paw.dolly.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.dolly.api.plugins.configureAuthentication
import no.nav.paw.dolly.api.plugins.configureHTTP
import no.nav.paw.dolly.api.plugins.configureKafka
import no.nav.paw.dolly.api.plugins.configureRouting
import no.nav.paw.dolly.api.plugins.configureSerialization
import no.nav.paw.dolly.api.plugins.configureTracing
import no.nav.paw.dolly.api.utils.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin

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
    installWebAppMetricsPlugin(applicationContext.prometheusMeterRegistry)
    configureAuthentication(applicationContext)
    configureHTTP()
    configureSerialization()
    configureKafka(applicationContext)
    configureRouting(applicationContext)
    configureTracing()
    installLoggingPlugin()
}