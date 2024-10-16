package no.nav.paw.bekreftelsetjeneste

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.topology.buildTopology
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class ApplicationTestContext(initialWallClockTime: Instant = Instant.now()) {
    val ansvarsTopicSerde: Serde<AnsvarEndret> = opprettSerde()
    val bekreftelseSerde: Serde<Bekreftelse> = opprettSerde()
    val periodeTopicSerde: Serde<Periode> = opprettSerde()
    val hendelseLoggSerde: Serde<BekreftelseHendelse> = BekreftelseHendelseSerde()
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaKeysClient = inMemoryKafkaKeysMock()
    val applicationContext = ApplicationContext(
        applicationConfig = applicationConfig,
        prometheusMeterRegistry = mockk<PrometheusMeterRegistry>(),
        healthIndicatorRepository = HealthIndicatorRepository(),
        kafkaKeysClient = kafkaKeysClient
    )

    val logger: Logger = LoggerFactory.getLogger(ApplicationTestContext::class.java)

    val topology = buildTopology(
        applicationContext = applicationContext,
        keyValueStateStoreSupplier = Stores::inMemoryKeyValueStore
    )

    val testDriver: TopologyTestDriver = TopologyTestDriver(topology, kafkaStreamProperties, initialWallClockTime)

    val periodeTopic: TestInputTopic<Long, Periode> = testDriver.createInputTopic(
        applicationConfig.kafkaTopology.periodeTopic,
        Serdes.Long().serializer(),
        periodeTopicSerde.serializer()
    )

    val bekreftelseTopic: TestInputTopic<Long, Bekreftelse> = testDriver.createInputTopic(
        applicationConfig.kafkaTopology.bekreftelseTopic,
        Serdes.Long().serializer(),
        bekreftelseSerde.serializer()
    )

    val bekreftelseHendelseloggTopicOut: TestOutputTopic<Long, BekreftelseHendelse> = testDriver.createOutputTopic(
        applicationConfig.kafkaTopology.bekreftelseHendelseloggTopic,
        Serdes.Long().deserializer(),
        hendelseLoggSerde.deserializer()
    )
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