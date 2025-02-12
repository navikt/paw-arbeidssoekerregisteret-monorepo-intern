package no.nav.paw.bekreftelse

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.paw.bekreftelse.context.ApplicationContext
import no.nav.paw.bekreftelse.plugins.configureRouting
import no.nav.paw.bekreftelse.utils.buildApplicationLogger
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.kafka.plugin.installKafkaStreamsPlugins
import no.nav.paw.metrics.plugin.installMetricsPlugin

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

fun Application.module(applicationContext: ApplicationContext) {
    with(applicationContext) {
        installMetricsPlugin(
            meterRegistry = prometheusMeterRegistry,
            additionalMeterBinders = kafkaStreamsList.map { KafkaStreamsMetrics(it) }
        )
        installKafkaStreamsPlugins(
            kafkaStreamsList = kafkaStreamsList
        )
        configureRouting(applicationContext)
    }
}
