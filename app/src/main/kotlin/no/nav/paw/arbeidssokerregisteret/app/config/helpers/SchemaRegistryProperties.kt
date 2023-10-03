package no.nav.paw.arbeidssokerregisteret.app.config.helpers;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig

class SchemaRegistryProperties (
        val url: String,
        val username: String?,
        val password: String?,
        val autoRegisterSchema: Boolean = true,
        val kafkaAvroSpecificReaderConfig: Boolean = true
): KafkaProperties {
        init {
                require(url.isNotBlank()) { "'url' kan ikke være tom(eller bare bestå av mellomrom)" }
        }

        override val map: Map<String, Any>
                get() = mapOf(
                        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to url,
                        SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                        SchemaRegistryClientConfig.USER_INFO_CONFIG to "$username:$password",
                        AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS to autoRegisterSchema,
                        KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to kafkaAvroSpecificReaderConfig
                )
}

fun Map<String, String>.SchemaRegistryProperties(
        autoRegisterSchema: Boolean = true,
        kafkaAvroSpecificReaderConfig: Boolean = true
): SchemaRegistryProperties =
        SchemaRegistryProperties(
                url = konfigVerdi("KAFKA_SCHEMA_REGISTRY"),
                username = konfigVerdi("KAFKA_SCHEMA_REGISTRY_USER"),
                password = konfigVerdi("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
                autoRegisterSchema = autoRegisterSchema,
                kafkaAvroSpecificReaderConfig = kafkaAvroSpecificReaderConfig
        )
