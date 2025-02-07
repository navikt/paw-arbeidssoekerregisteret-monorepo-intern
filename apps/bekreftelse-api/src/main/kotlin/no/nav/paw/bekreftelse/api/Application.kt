package no.nav.paw.bekreftelse.api

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.plugins.configureRouting
import no.nav.paw.bekreftelse.api.plugins.installWebPlugins
import no.nav.paw.bekreftelse.api.plugins.installKafkaPlugins
import no.nav.paw.bekreftelse.api.plugins.installTracingPlugin
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installMetricsPlugin
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
    installWebPlugins(applicationContext)
    installLoggingPlugin()
    installContentNegotiationPlugin()
    installMetricsPlugin(
        meterRegistry = applicationContext.prometheusMeterRegistry,
        additionalMeterBinders = applicationContext.additionalMeterBinders
    )
    installTracingPlugin()
    installAuthenticationPlugin(providers = applicationContext.securityConfig.authProviders)
    installDatabasePlugin(dataSource = applicationContext.dataSource)
    installKafkaPlugins(applicationContext)
    configureRouting(applicationContext)
}
