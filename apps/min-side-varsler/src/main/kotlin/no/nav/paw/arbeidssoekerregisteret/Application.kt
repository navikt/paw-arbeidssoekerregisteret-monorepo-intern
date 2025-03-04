package no.nav.paw.arbeidssoekerregisteret

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.plugin.configureRouting
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.kafka.plugin.installKafkaStreamsPlugins
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.metrics.plugin.installMetricsPlugin

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
        val additionalMeterBinders = kafkaStreamsList.map { KafkaStreamsMetrics(it) }
        installMetricsPlugin(prometheusMeterRegistry, additionalMeterBinders)
        installDatabasePlugin(dataSource)
        installKafkaStreamsPlugins(kafkaStreamsList, kafkaStreamsShutdownTimeout)
        configureRouting(healthIndicatorRepository, prometheusMeterRegistry)
    }
}
