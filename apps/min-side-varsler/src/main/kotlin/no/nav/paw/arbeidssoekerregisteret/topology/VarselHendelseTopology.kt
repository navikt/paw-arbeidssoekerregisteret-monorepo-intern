package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.streams.addVarselHendelseStream
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseSerde
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

fun buildVarselHendelseTopology(
    serverConfig: ServerConfig,
    applicationConfig: ApplicationConfig,
    kafkaConfig: KafkaConfig,
    meterRegistry: MeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    varselService: VarselService
): KafkaStreams {
    val kafkaTopology = StreamsBuilder()
        .addVarselHendelseStream(
            runtimeEnvironment = serverConfig.runtimeEnvironment,
            applicationConfig = applicationConfig,
            meterRegistry = meterRegistry,
            varselService = varselService
        )
        .build()
    val kafkaStreamsFactory = KafkaStreamsFactory(applicationConfig.varselHendelseStreamSuffix, kafkaConfig)
        .withDefaultKeySerde(Serdes.String()::class)
        .withDefaultValueSerde(VarselHendelseSerde::class)
    return KafkaStreams(kafkaTopology, kafkaStreamsFactory.properties)
        .withApplicationTerminatingExceptionHandler()
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository.livenessIndicator(),
            readinessIndicator = healthIndicatorRepository.readinessIndicator()
        )
}