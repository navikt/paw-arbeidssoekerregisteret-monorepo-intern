package no.nav.paw.bekreftelsetjeneste

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class ApplicationTestContext {
    val ansvarsTopicSerde: Serde<AnsvarEndret> = opprettSerde()
    val bekreftelseSerde: Serde<Bekreftelse> = opprettSerde()
    val periodeTopicSerde: Serde<Periode> = opprettSerde()
    val hendelseLoggSerde: Serde<BekreftelseHendelse> = BekreftelseHendelseSerde()
    val applicationConfiguration = ApplicationConfiguration(
        periodeTopic = "periodeTopic",
        ansvarsTopic = "ansvarsTopic",
        bekreftelseTopic = "bekreftelseTopic",
        bekreftelseHendelseloggTopic = "bekreftelseHendelsesloggTopic",
        stateStoreName = "stateStoreName",
        punctuateInterval = Duration.ofSeconds(1)
    )
    val applicationContext = ApplicationContext(
        internTilstandSerde = InternTilstandSerde(),
        bekreftelseHendelseSerde = BekreftelseHendelseSerde(),
        kafkaKeysClient = inMemoryKafkaKeysMock()
    )

    val logger = LoggerFactory.getLogger(ApplicationTestContext::class.java)

    val testDriver: TopologyTestDriver =
        with(applicationContext) {
            with(applicationConfiguration) {
                StreamsBuilder()
                    .addStateStore(
                        KeyValueStoreBuilder(
                            InMemoryKeyValueBytesStoreSupplier(applicationConfiguration.stateStoreName),
                            Serdes.UUID(),
                            applicationContext.internTilstandSerde,
                            Time.SYSTEM
                        )
                    ).appTopology()
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

    val bekreftelseTopic = testDriver.createInputTopic(
        applicationConfiguration.bekreftelseTopic,
        Serdes.Long().serializer(),
        bekreftelseSerde.serializer()
    )

    val hendelseLoggTopic = testDriver.createOutputTopic(
        applicationConfiguration.bekreftelseHendelseloggTopic,
        Serdes.Long().deserializer(),
        hendelseLoggSerde.deserializer()
    )

    fun kafkaKeyFunction(id: String): KafkaKeysResponse = applicationContext.kafkaKeyFunction(id)
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