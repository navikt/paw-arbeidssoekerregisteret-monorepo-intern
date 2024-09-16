package no.nav.paw.bekreftelse.api.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
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

fun buildInternStateSerde() = buildJsonSerde<InternState>()

fun buildBekreftelseSerde() = SpecificAvroSerde<Bekreftelse>()
