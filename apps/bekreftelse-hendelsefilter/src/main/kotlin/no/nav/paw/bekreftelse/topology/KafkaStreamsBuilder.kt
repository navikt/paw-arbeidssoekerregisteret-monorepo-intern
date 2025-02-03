package no.nav.paw.bekreftelse.topology

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.error.handler.KafkaLogAndContinueExceptionHandler
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology

fun buildKafkaStreamsFactory(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applicationIdSuffix: ApplicationIdSuffix,
    kafkaConfig: KafkaConfig
): KafkaStreamsFactory {
    return KafkaStreamsFactory(applicationIdSuffix.value, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .addPrometheusMeterRegistryToConfig(prometheusMeterRegistry)
        .withSerializationExceptionHendler(KafkaLogAndContinueExceptionHandler::class)
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
