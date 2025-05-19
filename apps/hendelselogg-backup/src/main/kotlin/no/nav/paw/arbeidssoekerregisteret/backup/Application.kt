package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.customExceptionResolver
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.plugin.configureRouting
import no.nav.paw.arbeidssoekerregisteret.backup.plugin.installHwmPlugin
import no.nav.paw.arbeidssoekerregisteret.backup.plugin.installKafkaPlugin
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

fun main() {
    val logger = buildApplicationLogger
    val applicationContext = ApplicationContext.create()

    with(applicationContext.serverConfig) {
        val appName = runtimeEnvironment.appNameOrDefaultForLocal()
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

fun Application.module(applicationContext: ApplicationContext) =
    with(applicationContext) {
        installLoggingPlugin()
        installContentNegotiationPlugin()
        installWebAppMetricsPlugin(
            meterRegistry = prometheusMeterRegistry,
            additionalMeterBinders = listOf(additionalMeterBinder)
        )
        installDatabasePlugin(dataSource)
        installKafkaPlugin(
            applicationContext = this,
            hendelseKafkaConsumer = hendelseKafkaConsumer
        )
        installErrorHandlingPlugin(
            customResolver = customExceptionResolver()
        )
        installAuthenticationPlugin(securityConfig.authProviders)
        installHwmPlugin(applicationContext)
        configureRouting(
            meterRegistry = prometheusMeterRegistry,
            brukerstoetteService = brukerstoetteService
        )
    }
