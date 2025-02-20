package no.nav.paw.serialization.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.serialization.jackson.buildObjectMapper
import org.apache.kafka.common.serialization.Serializer

open class JacksonSerializer<T : Any>(
    private val objectMapper: ObjectMapper = buildObjectMapper,
    private val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
) : Serializer<T> {
    override fun serialize(topic: String?, data: T?): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T : Any> buildJacksonSerializer(
    objectMapper: ObjectMapper = buildObjectMapper,
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): JacksonSerializer<T> {
    return JacksonSerializer(objectMapper, runtimeEnvironment)
}
