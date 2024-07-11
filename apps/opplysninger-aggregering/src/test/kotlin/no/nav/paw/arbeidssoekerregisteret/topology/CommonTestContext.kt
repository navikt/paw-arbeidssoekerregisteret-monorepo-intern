package no.nav.paw.arbeidssoekerregisteret.topology

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.properties.ApplicationProperties
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

open class CommonTestContext {

    val logger: Logger = LoggerFactory.getLogger("no.nav.paw.test")
    val applicationProperties =
        loadNaisOrLocalConfiguration<ApplicationProperties>("test_application_configuration.toml")
    val meterRegistry = SimpleMeterRegistry()

    val kafkaStreamProperties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = applicationProperties.kafka.applicationIdPrefix
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = applicationProperties.kafka.brokers
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = MockSchemaRegistryAvroSerde<SpecificRecord>().javaClass
        this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
        this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = applicationProperties.kafka.schemaRegistry?.url
    }

    class MockSchemaRegistryAvroSerde<T : SpecificRecord> :
        SpecificAvroSerde<T>(MockSchemaRegistry.getClientForScope("test")) {
        init {
            this.configure(
                mapOf(
                    KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                    KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://dummy:1234"
                ), false
            )
        }
    }

    fun <K, V> KeyValueStore<K, V>.size(): Int {
        var count = 0
        for (keyValue in all()) {
            count++
        }
        return count
    }
}
