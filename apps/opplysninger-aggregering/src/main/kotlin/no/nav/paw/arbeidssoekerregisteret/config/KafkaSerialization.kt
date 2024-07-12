package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaSchemaRegistryConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

inline fun <reified T> buildJsonSerializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonDeserializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue<T>(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonSerde(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(naisEnv, objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(naisEnv, objectMapper)
    }
}

inline fun <reified T> buildJsonSerde(): Serde<T> {
    return buildJsonSerde<T>(currentNaisEnv, buildObjectMapper)
}

inline fun <reified T : SpecificRecord> buildAvroSerde(config: KafkaSchemaRegistryConfig?): Serde<T> {
    val serdeConfig = buildAvroSerdeConfig(config)
    val serde = SpecificAvroSerde<T>()
    serde.configure(serdeConfig, false)
    return serde
}

fun buildOpplysningerOmArbeidssoekerAvroSerde(config: KafkaSchemaRegistryConfig?): Serde<OpplysningerOmArbeidssoeker> {
    return buildAvroSerde(config)
}

fun buildAvroSerdeConfig(config: KafkaSchemaRegistryConfig?): Map<String, Any> {
    return if (config == null) {
        emptyMap()
    } else {
        buildMap {
            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, config.url)
            put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, config.autoRegisterSchema)
            put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, config.avroSpecificReaderConfig)
            if (config.username != null) {
                put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
                put(SchemaRegistryClientConfig.USER_INFO_CONFIG, "${config.username}:${config.password}")
            }
        }
    }
}

