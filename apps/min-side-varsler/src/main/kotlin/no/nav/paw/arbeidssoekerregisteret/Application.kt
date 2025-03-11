package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugin.configureRouting
import no.nav.paw.arbeidssoekerregisteret.plugin.installScheduledTaskPlugin
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.kafka.plugin.installKafkaStreamsPlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.build()
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
    with(applicationContext) {
        installLoggingPlugin()
        installContentNegotiationPlugin()
        installErrorHandlingPlugin()
        val additionalMeterBinders = kafkaStreamsList.map { KafkaStreamsMetrics(it) }
        installMetricsPlugin(prometheusMeterRegistry, additionalMeterBinders)
        installAuthenticationPlugin(securityConfig.authProviders)
        installDatabasePlugin(dataSource)
        installKafkaStreamsPlugins(kafkaStreamsList, kafkaShutdownTimeout)
        installScheduledTaskPlugin(applicationConfig, bestillingService)
        configureRouting(healthIndicatorRepository, prometheusMeterRegistry, varselService, bestillingService)
    }
}
