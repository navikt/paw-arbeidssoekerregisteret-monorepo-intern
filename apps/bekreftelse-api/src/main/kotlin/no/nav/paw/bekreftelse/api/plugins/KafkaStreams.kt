package no.nav.paw.bekreftelse.api.plugins

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.ServerConfig
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
    serverConfig: ServerConfig,
    applicationConfig: ApplicationConfig,
    healthIndicatorRepository: HealthIndicatorRepository,
    topology: Topology
): KafkaStreams {
    val livenessIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator())
    val readinessIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())

    val streamsFactory = KafkaStreamsFactory(
        applicationIdSuffix = applicationConfig.kafkaTopology.applicationIdSuffix,
        config = applicationConfig.kafkaClients,
    )
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withServerConfig(serverConfig.ip, serverConfig.port)

    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsFactory.properties)
    )
    kafkaStreams.withHealthIndicatorStateListener(livenessIndicator, readinessIndicator)
    kafkaStreams.withApplicationTerminatingExceptionHandler()
    return kafkaStreams
}
