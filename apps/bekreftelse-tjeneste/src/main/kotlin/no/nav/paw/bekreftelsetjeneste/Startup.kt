package no.nav.paw.bekreftelsetjeneste

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores
import java.time.Duration

const val APPLICATION_ID_SUFFIX = "beta"

fun main() {
    val applicationConfiguration = ApplicationConfiguration(
        periodeTopic = "paw.arbeidssokerperioder-v1",
        ansvarsTopic = "paw.arbeidssoker-bekreftelse-ansvar-beta-v1",
        bekreftelseTopic = "paw.arbeidssoker-bekreftelse-beta-v1",
        bekreftelseHendelseloggTopic = "paw.arbeidssoker-bekreftelse-hendelseslogg-beta-v1",
        stateStoreName = "bekreftelse",
        punctuateInterval = Duration.ofMinutes(5)
    )
    val applicationContext: ApplicationContext = ApplicationContext(
        InternTilstandSerde(),
        BekreftelseHendelseSerde(),
        createKafkaKeyGeneratorClient()
    )
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfiguration.stateStoreName),
                Serdes.UUID(),
                InternTilstandSerde()
            )
        )
    val topology = with(applicationContext) {
        with(applicationConfiguration) {
            streamsBuilder.appTopology()
        }
    }
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val streamsFactory = KafkaStreamsFactory(APPLICATION_ID_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
    val streams = KafkaStreams(topology, streamsFactory.properties)
}