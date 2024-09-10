package no.nav.paw.meldeplikttjeneste

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstandSerde
import no.nav.paw.rapportering.ansvar.v1.AnsvarEndret
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelseSerde
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ApplicationTestContext {
    val ansvarsTopicSerde: Serde<AnsvarEndret> = opprettSerde()
    val rapporteringMeldingSerde: Serde<Melding> = opprettSerde()
    val periodeTopicSerde: Serde<Periode> = opprettSerde()
    val hendelseLoggSerde: Serde<RapporteringsHendelse> = RapporteringsHendelseSerde()
    val applicationConfiguration = ApplicationConfiguration(
        periodeTopic = "periodeTopic",
        ansvarsTopic = "ansvarsTopic",
        rapporteringsTopic = "rapporteringsTopic",
        rapporteringsHendelsesloggTopic = "rapporteringsHendelsesloggTopic",
        statStoreName = "statStoreName",
        punctuateInterval = Duration.ofSeconds(1)
    )
    val applicationContext = ApplicationContext(
        internTilstandSerde = InternTilstandSerde(),
        rapporteringsHendelseSerde = RapporteringsHendelseSerde()
    )

    val kafkaKeysService = kafkaKeyInstance

    val testDriver: TopologyTestDriver =
        with(applicationContext) {
            with(applicationConfiguration) {
                StreamsBuilder()
                    .addStateStore(
                        KeyValueStoreBuilder(
                            InMemoryKeyValueBytesStoreSupplier(applicationConfiguration.statStoreName),
                            Serdes.UUID(),
                            applicationContext.internTilstandSerde,
                            Time.SYSTEM
                        )
                    ).appTopology(kafkaKeysService)
            }
        }.let { TopologyTestDriver(it, kafkaStreamProperties) }

    val periodeTopic = testDriver.createInputTopic(
        applicationConfiguration.periodeTopic,
        Serdes.Long().serializer(),
        periodeTopicSerde.serializer()
    )

    val ansvarsTopic = testDriver.createInputTopic(
        applicationConfiguration.ansvarsTopic,
        Serdes.Long().serializer(),
        ansvarsTopicSerde.serializer()
    )

    val rapporteringsTopic = testDriver.createInputTopic(
        applicationConfiguration.rapporteringsTopic,
        Serdes.Long().serializer(),
        rapporteringMeldingSerde.serializer()
    )

    val hendelseLoggTopic = testDriver.createOutputTopic(
        applicationConfiguration.rapporteringsHendelsesloggTopic,
        Serdes.Long().deserializer(),
        hendelseLoggSerde.deserializer()
    )
}

val kafkaKeyInstance: (String) -> KafkaKeysResponse
    get() {
        val map = ConcurrentHashMap<String, KafkaKeysResponse>()
        val sequence = AtomicLong(0)
        return { key ->
            map.computeIfAbsent(key) {
                val id = sequence.getAndIncrement()
                KafkaKeysResponse(sequence.getAndIncrement(), id % 2)
            }
        }
    }

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"

fun <T : SpecificRecord> opprettSerde(): Serde<T> {
    val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
    val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
    serde.configure(
        mapOf(
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
        ),
        false
    )
    return serde
}

val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
}