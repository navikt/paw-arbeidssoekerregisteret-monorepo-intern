package no.nav.paw.bekreftelse.api.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

inline fun <reified T> buildJsonSerializer(runtimeEnvironment: RuntimeEnvironment, objectMapper: ObjectMapper) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonDeserializer(runtimeEnvironment: RuntimeEnvironment, objectMapper: ObjectMapper) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue<T>(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonSerde(runtimeEnvironment: RuntimeEnvironment, objectMapper: ObjectMapper) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(runtimeEnvironment, objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(runtimeEnvironment, objectMapper)
    }
}

inline fun <reified T> buildJsonSerde(): Serde<T> {
    return buildJsonSerde<T>(currentRuntimeEnvironment, buildObjectMapper)
}

fun buildInternStateSerde() = buildJsonSerde<InternState>()

fun buildBekreftelseSerde() = SpecificAvroSerde<Bekreftelse>()
