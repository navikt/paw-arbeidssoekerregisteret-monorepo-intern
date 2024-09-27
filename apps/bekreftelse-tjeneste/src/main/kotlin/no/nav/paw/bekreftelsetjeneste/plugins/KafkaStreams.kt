package no.nav.paw.bekreftelsetjeneste.plugins

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.util.*
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import java.time.Duration

@KtorDsl
class KafkaStreamsPluginConfig {
    var shutDownTimeout: Duration? = null
    var kafkaStreams: List<KafkaStreams>? = null
}

val KafkaStreamsPlugin: ApplicationPlugin<KafkaStreamsPluginConfig> =
    createApplicationPlugin("KafkaStreams", ::KafkaStreamsPluginConfig) {
        val shutDownTimeout = requireNotNull(pluginConfig.shutDownTimeout) { "ShutDownTimeout er null" }
        val kafkaStreams = requireNotNull(pluginConfig.kafkaStreams) { "KafkaStreams er null" }

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Starter Kafka Streams")
            kafkaStreams.forEach { stream -> stream.start() }
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Stopper Kafka Streams")
            kafkaStreams.forEach { stream -> stream.close(shutDownTimeout) }
        }
    }

fun buildKafkaStreams(
    applicationConfig: ApplicationConfig,
    healthIndicatorRepository: HealthIndicatorRepository,
    topology: Topology
): KafkaStreams {
    val livenessIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
    val readinessIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

    val streamsFactory = KafkaStreamsFactory(
        applicationConfig.kafkaTopology.applicationIdSuffix,
        applicationConfig.kafkaStreams
    )
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .apply { properties["application.server"] = applicationConfig.hostname }

    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsFactory.properties)
    )
    kafkaStreams.withHealthIndicatorStateListener(livenessIndicator, readinessIndicator)
    kafkaStreams.withApplicationTerminatingExceptionHandler()
    return kafkaStreams
}
