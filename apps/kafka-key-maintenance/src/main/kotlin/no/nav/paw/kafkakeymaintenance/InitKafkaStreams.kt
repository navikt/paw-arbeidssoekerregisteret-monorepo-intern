package no.nav.paw.kafkakeymaintenance

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.error.handler.withApplicationTerminatingExceptionHandler
import no.nav.paw.health.listener.withHealthIndicatorStateListener
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorTopologyConfig
import no.nav.paw.kafkakeymaintenance.pdlprocessor.buildAktorTopology
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.Stores

fun initStreams(
    applicationContext: ApplicationContext,
    aktorTopologyConfig: AktorTopologyConfig,
    healthIndicatorRepository: HealthIndicatorRepository,
    perioder: Perioder,
    hentAlias: (List<String>) -> List<LokaleAlias>
): KafkaStreams {
    val kafkaStreamsFactory = KafkaStreamsFactory(
        "beta-v2",
        loadNaisOrLocalConfiguration(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    ).withExactlyOnce()
        .withDefaultKeySerde(Serdes.StringSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val topology = initTopology(
        meterRegistry = applicationContext.meterRegistry,
        aktorSerde = kafkaStreamsFactory.createSpecificAvroSerde<Aktor>(),
        aktorTopologyConfig = aktorTopologyConfig,
        perioder = perioder,
        hentAlias = hentAlias,
        stateStoreBuilderFactory = Stores::persistentTimestampedKeyValueStore
    )

    val streams = KafkaStreams(topology, kafkaStreamsFactory.properties)
    streams
        .withHealthIndicatorStateListener(
            livenessIndicator = healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator()),
            readinessIndicator = healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator())
        )
    streams.setUncaughtExceptionHandler { throwable ->
        applicationContext.logger.error("Uncaught exception in Kafka Streams", throwable)
        applicationContext.shutdownCalled.set(true)
        SHUTDOWN_APPLICATION
    }
    return streams
}

fun initTopology(
    meterRegistry: PrometheusMeterRegistry,
    stateStoreBuilderFactory: (String) -> KeyValueBytesStoreSupplier,
    aktorTopologyConfig: AktorTopologyConfig,
    perioder: Perioder,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktorSerde: Serde<Aktor>
): Topology {
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.timestampedKeyValueStoreBuilder(
                stateStoreBuilderFactory(aktorTopologyConfig.stateStoreName),
                Serdes.StringSerde(),
                aktorSerde
            )
        )
    streamsBuilder.buildAktorTopology(
        meterRegistry = meterRegistry,
        aktorSerde = aktorSerde,
        aktorTopologyConfig = aktorTopologyConfig,
        perioder = perioder,
        hentAlias = hentAlias
    )
    return streamsBuilder.build()
}