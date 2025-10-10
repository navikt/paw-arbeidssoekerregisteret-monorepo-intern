package no.nav.paw.kafkakeygenerator

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.kafkakeygenerator.context.ApplicationContext
import no.nav.paw.kafkakeygenerator.plugin.configureRouting
import no.nav.paw.kafkakeygenerator.plugin.installCustomLoggingPlugin
import no.nav.paw.kafkakeygenerator.plugin.installKafkaPlugins
import no.nav.paw.kafkakeygenerator.plugin.installScheduledTaskPlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

fun main() {
    val logger = buildApplicationLogger
    val applicationContext = ApplicationContext.create()

    with(applicationContext.serverConfig) {
        val appName = runtimeEnvironment.appNameOrDefaultForLocal()
        logger.info("Starter {} med hostname {} og port {}", appName, host, port)

        embeddedServer(factory = Netty, port = port) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter {}", appName)
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    with(applicationContext) {
        installCustomLoggingPlugin()
        installContentNegotiationPlugin()
        installErrorHandlingPlugin()
        installAuthenticationPlugin(securityConfig.authProviders)
        installWebAppMetricsPlugin(
            meterRegistry = prometheusMeterRegistry,
            additionalMeterBinders = additionalMeterBinders
        )
        installDatabasePlugin(dataSource)
        installKafkaPlugins(
            applicationConfig = applicationConfig,
            pawPeriodeConsumer = pawPeriodeConsumer,
            pawPeriodeConsumerExceptionHandler = pawPeriodeConsumerExceptionHandler,
            pawPeriodeHwmRebalanceListener = pawPeriodeConsumerRebalanceListener,
            pawPeriodeKafkaConsumerService = pawPeriodeKafkaConsumerService,
            pdlAktorConsumer = pdlAktorConsumer,
            pdlAktorConsumerExceptionHandler = pdlAktorConsumerExceptionHandler,
            pdlAktorHwmRebalanceListener = pdlAktorConsumerRebalanceListener,
            pdlAktorKafkaConsumerService = pdlAktorKafkaConsumerService
        )
        installScheduledTaskPlugins(
            applicationConfig = applicationConfig,
            konfliktService = konfliktService
        )
        configureRouting(
            meterRegistry = prometheusMeterRegistry,
            healthIndicatorRepository = healthIndicatorRepository,
            kafkaKeysService = kafkaKeysService,
            identitetResponseService = identitetResponseService
        )
    }
}
