package no.nav.paw.dolly.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.dolly.api.plugin.configureRouting
import no.nav.paw.dolly.api.plugin.installKafkaPlugin
import no.nav.paw.dolly.api.plugin.installTracingPlugin
import no.nav.paw.dolly.api.plugin.installWebPlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

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
    installWebPlugins()
    installContentNegotiationPlugin()
    installLoggingPlugin()
    installTracingPlugin()
    installWebAppMetricsPlugin(applicationContext.prometheusMeterRegistry)
    installAuthenticationPlugin(applicationContext.securityConfig.authProviders)
    installKafkaPlugin(applicationContext)
    configureRouting(applicationContext)
}