package no.nav.paw.serialization.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.serialization.jackson.buildObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class JacksonDeserializer<T : Any>(
    private val clazz: KClass<T>,
    private val objectMapper: ObjectMapper = buildObjectMapper,
    private val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
) : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue(data, clazz.java)
        } catch (e: Exception) {
            if (runtimeEnvironment is ProdGcp && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T : Any> buildJacksonDeserializer(
    objectMapper: ObjectMapper = buildObjectMapper,
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): JacksonDeserializer<T> {
    return JacksonDeserializer(T::class, objectMapper, runtimeEnvironment)
}
