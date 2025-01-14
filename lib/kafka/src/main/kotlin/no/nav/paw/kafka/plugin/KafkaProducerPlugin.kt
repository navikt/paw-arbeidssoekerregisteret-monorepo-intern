package no.nav.paw.kafka.plugin

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import org.apache.kafka.clients.producer.Producer
import java.time.Duration

const val KAFKA_PRODUCER_PLUGIN_NAME = "KafkaProducerPlugin"

class KafkaProducerPluginConfig {
    var kafkaProducers: List<Producer<*, *>>? = null
    var shutDownTimeout: Duration? = null
}

val KafkaProducerPlugin: ApplicationPlugin<KafkaProducerPluginConfig> =
    createApplicationPlugin(KAFKA_PRODUCER_PLUGIN_NAME, ::KafkaProducerPluginConfig) {
        application.log.info("Installerer {}", KAFKA_PRODUCER_PLUGIN_NAME)
        val kafkaProducers = requireNotNull(pluginConfig.kafkaProducers) { "KafkaProducers er null" }
        val shutDownTimeout = pluginConfig.shutDownTimeout ?: Duration.ofMillis(250)

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopper Kafka Producers")
            kafkaProducers.forEach { producer ->
                producer.close(shutDownTimeout)
            }
        }
    }