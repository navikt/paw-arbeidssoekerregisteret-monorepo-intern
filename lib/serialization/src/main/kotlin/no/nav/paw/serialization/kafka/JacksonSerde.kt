package no.nav.paw.serialization.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.serialization.jackson.buildObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

class JacksonSerde<T : Any>(
    private val clazz: KClass<T>,
    private val objectMapper: ObjectMapper = buildObjectMapper,
    private val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
) : Serde<T> {
    override fun serializer(): Serializer<T> {
        return JacksonSerializer(objectMapper, runtimeEnvironment)
    }

    override fun deserializer(): Deserializer<T> {
        return JacksonDeserializer(clazz, objectMapper, runtimeEnvironment)
    }

}

inline fun <reified T : Any> buildJacksonSerde(
    objectMapper: ObjectMapper = buildObjectMapper,
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
): JacksonSerde<T> {
    return JacksonSerde(T::class, objectMapper, runtimeEnvironment)
}
