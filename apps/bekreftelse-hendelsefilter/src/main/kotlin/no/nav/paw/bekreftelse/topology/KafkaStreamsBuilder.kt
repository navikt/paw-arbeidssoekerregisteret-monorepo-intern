package no.nav.paw.bekreftelse.topology

import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology

fun KafkaStreamsFactory.buildKafkaStreamsList(
    applicationConfig: ApplicationConfig,
    healthIndicatorRepository: HealthIndicatorRepository
): List<KafkaStreams> {
    val bekreftelseTopologyList = buildBekreftelseKafkaTopologyList(applicationConfig)
    val bekreftelsePaaVegneAvTopologyList = buildBekreftelsePaaVegneAvKafkaTopologyList(applicationConfig)
    val kafkaTopologyList = bekreftelseTopologyList + bekreftelsePaaVegneAvTopologyList
    return kafkaTopologyList.map { buildKafkaStreams(healthIndicatorRepository, it) }
}

fun KafkaStreamsFactory.buildKafkaStreams(
    healthIndicatorRepository: HealthIndicatorRepository,
    kafkaTopology: Topology
): KafkaStreams {
    return KafkaStreams(
        kafkaTopology,
        StreamsConfig(properties)
    )
        .withHealthIndicatorStateListener(
            healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator()),
            healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
        )
        .withApplicationTerminatingExceptionHandler()
}