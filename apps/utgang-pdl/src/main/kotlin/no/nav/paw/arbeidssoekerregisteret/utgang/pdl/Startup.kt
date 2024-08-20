package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentForenkletStatus
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.health.Health
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.health.initKtor
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.appTopology
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseStateSerde
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("utgang-pdl")
    logger.info("Starter: {}", ApplicationInfo.id)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val applicationConfiguration = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)

    val streamsConfig = KafkaStreamsFactory(
        applicationIdSuffix = applicationConfiguration.applicationIdSuffix,
        config = kafkaConfig
    )
        .withDefaultKeySerde(Serdes.LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfiguration.hendelseStateStoreName),
                Serdes.UUID(),
                HendelseStateSerde()
            )
        )

    val topology = streamsBuilder.appTopology(
        prometheusMeterRegistry,
        applicationConfiguration.periodeTopic,
        applicationConfiguration.hendelseloggTopic,
        applicationConfiguration.hendelseStateStoreName,
        pdlHentForenkletStatus = PdlHentForenkletStatus.create(),
        pdlHentPerson = PdlHentPerson.create(),
    )
    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsConfig.properties)
    )
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: ${throwable.message}", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()

    initKtor(
        kafkaStreamsMetrics = KafkaStreamsMetrics(kafkaStreams),
        prometheusRegistry = prometheusMeterRegistry,
        health = Health(kafkaStreams)
    ).start(wait = true)
    logger.info("Avsluttet")
}
