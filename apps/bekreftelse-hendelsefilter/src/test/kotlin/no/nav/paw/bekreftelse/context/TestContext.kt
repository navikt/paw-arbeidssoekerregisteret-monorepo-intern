package no.nav.paw.bekreftelse.context

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.topology.buildKafkaTopology
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import java.util.*

class TestContext {
    private val schemaRegistryScope = "test-registry"
    private val bekreftelseSourceTopicName = "bekreftelse-source"
    private val bekreftelseTargetTopicName = "bekreftelse-target"
    private val bekreftelsePaaVegneAvSourceTopicName = "bekreftelse-paavegneav-source"
    private val bekreftelsePaaVegneAvTargetTopicName = "bekreftelse-paavegneav-target"

    private val kafkaStreamProperties = Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "test-kafka-streams"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
        this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
        this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$schemaRegistryScope"
    }
    private val schemaRegistryClient = MockSchemaRegistry.getClientForScope(schemaRegistryScope)
    private val bekreftelseAvroSerde: Serde<Bekreftelse> = avroSerde()
    private val bekreftelsePaaVegneAvAvroSerde: Serde<PaaVegneAv> = avroSerde()

    private val bekreftelseTestDriver = TopologyTestDriver(
        buildKafkaTopology<Bekreftelse>(bekreftelseSourceTopicName, bekreftelseTargetTopicName),
        kafkaStreamProperties
    )

    private val bekreftelsePaaVegneAvTestDriver = TopologyTestDriver(
        buildKafkaTopology<PaaVegneAv>(bekreftelsePaaVegneAvSourceTopicName, bekreftelsePaaVegneAvTargetTopicName),
        kafkaStreamProperties
    )

    val bekreftelseSourceTopic = bekreftelseTestDriver
        .createInputTopic(
            bekreftelseSourceTopicName,
            Serdes.Long().serializer(),
            bekreftelseAvroSerde.serializer()
        )
    val bekreftelseTargetTopic = bekreftelseTestDriver
        .createOutputTopic(
            bekreftelseTargetTopicName,
            Serdes.Long().deserializer(),
            bekreftelseAvroSerde.deserializer()
        )
    val bekreftelsePaaVegneAvSourceTopic = bekreftelsePaaVegneAvTestDriver
        .createInputTopic(
            bekreftelsePaaVegneAvSourceTopicName,
            Serdes.Long().serializer(),
            bekreftelsePaaVegneAvAvroSerde.serializer()
        )
    val bekreftelsePaaVegneAvTargetTopic = bekreftelsePaaVegneAvTestDriver
        .createOutputTopic(
            bekreftelsePaaVegneAvTargetTopicName,
            Serdes.Long().deserializer(),
            bekreftelsePaaVegneAvAvroSerde.deserializer()
        )

    private fun <T : SpecificRecord> avroSerde(): Serde<T> = SpecificAvroSerde<T>(schemaRegistryClient)
        .apply {
            configure(
                mapOf(
                    KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                    KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$schemaRegistryScope"
                ),
                false
            )
        }
}