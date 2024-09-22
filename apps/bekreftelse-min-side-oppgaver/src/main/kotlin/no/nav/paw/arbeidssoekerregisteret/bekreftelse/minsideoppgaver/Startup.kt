package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.applicationTopology
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.kafkaTopics
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.StateStoreName
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores

const val APP_SUFFIX = "beta"
val STATE_STORE_NAME: StateStoreName = StateStoreName("internal_state")


fun main() {
    val kafkaTopics = kafkaTopics()
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(STATE_STORE_NAME.value),
                Serdes.UUID(),
                jacksonSerde<InternTilstand>()
            )
        )
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val streamsFactory = KafkaStreamsFactory(APP_SUFFIX, kafkaConfig)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()
    val runtimeEvironment = currentRuntimeEnvironment
    val stream = KafkaStreams(streamsBuilder.applicationTopology(
        varselMeldingBygger = VarselMeldingBygger(runtimeEvironment),
        kafkaTopics = kafkaTopics,
        stateStoreName = STATE_STORE_NAME), streamsFactory.properties)
}
