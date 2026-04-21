package no.nav.paw.arbeidssoekerregisteret.topology

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addPeriodeStream
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

fun buildPeriodeTopology(
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .addPeriodeStream(
            applicationConfig = applicationConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        ).build()
    val kafkaStreamsFactory = KafkaStreamsFactory(applicationConfig.periodeStreamSuffix, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
    return KafkaStreams(kafkaTopology, kafkaStreamsFactory.properties)
        .withApplicationTerminatingExceptionHandler()
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository.livenessIndicator(),
            readinessIndicator = healthIndicatorRepository.readinessIndicator()
        )
}