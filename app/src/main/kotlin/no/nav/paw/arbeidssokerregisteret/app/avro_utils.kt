package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde

fun <T : SpecificRecord> lagSpecificAvroSerde(config: SchemaRegistryConfig): Serde<T> =
    SpecificAvroSerde<T>().apply {
        configure(
            mapOf(
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to config.url,
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                SchemaRegistryClientConfig.USER_INFO_CONFIG to "${config.bruker}:${config.passord}",
                AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to config.autoRegistrerSchema
            ), false
        )
    }