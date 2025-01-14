package no.nav.paw.kafka.plugin

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import org.apache.kafka.streams.KafkaStreams
import java.time.Duration

val KafkaStreamsStarting: EventDefinition<Application> = EventDefinition()
val KafkaStreamsStarted: EventDefinition<Application> = EventDefinition()
val KafkaStreamsStopping: EventDefinition<Application> = EventDefinition()
val KafkaStreamsStopped: EventDefinition<Application> = EventDefinition()

class KafkaStreamsPluginConfig {
    var kafkaStreams: List<KafkaStreams>? = null
    var shutDownTimeout: Duration? = null
}

val KafkaStreamsPlugin: ApplicationPlugin<KafkaStreamsPluginConfig> =
    createApplicationPlugin("KafkaStreams", ::KafkaStreamsPluginConfig) {
        val kafkaStreams = requireNotNull(pluginConfig.kafkaStreams) { "KafkaStreams er null" }
        val shutDownTimeout = pluginConfig.shutDownTimeout ?: Duration.ofSeconds(1)

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Starter Kafka Streams")
            application.monitor.raise(KafkaStreamsStarting, application)
            kafkaStreams.forEach { stream -> stream.start() }
            application.monitor.raise(KafkaStreamsStarted, application)
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopper Kafka Streams")
            application.monitor.raise(KafkaStreamsStopping, application)
            kafkaStreams.forEach { stream -> stream.close(shutDownTimeout) }
            application.monitor.raise(KafkaStreamsStopped, application)
        }
    }
