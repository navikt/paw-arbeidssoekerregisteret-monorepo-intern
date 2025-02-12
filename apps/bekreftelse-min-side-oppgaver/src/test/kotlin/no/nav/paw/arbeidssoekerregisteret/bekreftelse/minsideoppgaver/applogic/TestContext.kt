package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopicsConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.minSideVarselKonfigurasjon
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import no.nav.paw.serialization.kafka.buildJacksonSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.slf4j.LoggerFactory
import java.time.Period
import java.util.*

private fun StreamsBuilder.inMemStateStore(): StreamsBuilder {
    addStateStore(
        KeyValueStoreBuilder(
            InMemoryKeyValueBytesStoreSupplier(STATE_STORE_NAME.value),
            Serdes.UUID(),
            buildJacksonSerde<InternTilstand>(),
            Time.SYSTEM
        )
    )
    return this
}

data class TestContext(
    val periodeTopic: TestInputTopic<Long, Periode>,
    val bekreftelseHendelseTopic: TestInputTopic<Long, BekreftelseHendelse>,
    val tmsOppgaveTopic: TestOutputTopic<String, String>,
    val kevValueStore: KeyValueStore<Long, Period>,
    val topologyTestDriver: TopologyTestDriver,
    val kafkaKeyContext: KafkaKeyContext
) {
    val logger = LoggerFactory.getLogger("test.logger")
    operator fun <K, V> KeyValue<K, V>.component1(): K = key
    operator fun <K, V> KeyValue<K, V>.component2(): V = value

    companion object {
        fun build(
            runtimeEnvironment: RuntimeEnvironment = Local,
            varselMeldingBygger: VarselMeldingBygger = VarselMeldingBygger(
                runtimeEnvironment = runtimeEnvironment,
                minSideVarselKonfigurasjon = minSideVarselKonfigurasjon()
            ),
            kafkaKeyContext: KafkaKeyContext = KafkaKeyContext(inMemoryKafkaKeysMock()),
            kafkaTopicsConfig: KafkaTopicsConfig = KafkaTopicsConfig(
                periodeTopic = "periodeTopic",
                bekreftelseHendelseTopic = "bekreftelse-hendelse-topic",
                tmsOppgaveTopic = "tms-oppgave-topic",
                tmsVarselHendelseTopic = "tms-varsel-hendelse-topic"
            )
        ): TestContext {
            val periodeSerde = createAvroSerde<Periode>()
            val bekreftelseHendelseSerde = BekreftelseHendelseSerde()
            val topology = StreamsBuilder()
                .inMemStateStore()
                .bekreftelseKafkaTopology(
                    varselMeldingBygger = varselMeldingBygger,
                    kafkaTopicsConfig = kafkaTopicsConfig
                ).build()

            val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)

            val periodeInputTopic = testDriver.createInputTopic(
                kafkaTopicsConfig.periodeTopic,
                Serdes.Long().serializer(),
                periodeSerde.serializer()
            )
            val bekreftelseHendelseTopic = testDriver.createInputTopic(
                kafkaTopicsConfig.bekreftelseHendelseTopic,
                Serdes.Long().serializer(),
                bekreftelseHendelseSerde.serializer(),
            )
            val tmsOppgaveTopic = testDriver.createOutputTopic(
                kafkaTopicsConfig.tmsOppgaveTopic,
                Serdes.String().deserializer(),
                Serdes.String().deserializer()
            )
            return TestContext(
                periodeTopic = periodeInputTopic,
                bekreftelseHendelseTopic = bekreftelseHendelseTopic,
                tmsOppgaveTopic = tmsOppgaveTopic,
                kevValueStore = testDriver.getKeyValueStore(STATE_STORE_NAME.value),
                topologyTestDriver = testDriver,
                kafkaKeyContext = kafkaKeyContext
            )
        }
    }
}

const val SCHEMA_REGISTRY_SCOPE = "mock"
inline fun <reified T : SpecificRecord> createAvroSerde(): Serde<T> {

    return SpecificAvroSerde<T>(MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)).apply {
        configure(
            mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
            ),
            false
        )
    }
}

val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
}