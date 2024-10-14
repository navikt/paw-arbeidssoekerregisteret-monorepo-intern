package no.nav.paw.bekreftelse.api.plugins.custom

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import org.apache.kafka.clients.producer.Producer
import java.time.Duration

@KtorDsl
class KafkaProducerPluginConfig {
    var kafkaProducers: List<Producer<*, *>>? = null
    var shutDownTimeout: Duration = Duration.ofMillis(250)

    companion object {
        const val PLUGIN_NAME = "KafkaProducerPlugin"
    }
}

val KafkaProducerPlugin: ApplicationPlugin<KafkaProducerPluginConfig> =
    createApplicationPlugin(KafkaProducerPluginConfig.PLUGIN_NAME, ::KafkaProducerPluginConfig) {
        application.log.info("Oppretter {}", KafkaProducerPluginConfig.PLUGIN_NAME)
        val kafkaProducers = requireNotNull(pluginConfig.kafkaProducers) { "KafkaProducers er null" }
        val shutDownTimeout = requireNotNull(pluginConfig.shutDownTimeout) { "ShutDownTimeout er null" }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopper Kafka Producers")
            kafkaProducers.forEach { producer ->
                producer.close(shutDownTimeout)
            }
        }
    }