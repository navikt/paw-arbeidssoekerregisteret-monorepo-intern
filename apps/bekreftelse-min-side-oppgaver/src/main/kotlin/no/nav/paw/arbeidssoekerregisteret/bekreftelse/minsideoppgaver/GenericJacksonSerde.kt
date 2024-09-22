package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

inline fun <reified T: Any> jacksonSerde(mapper: ObjectMapper = ObjectMapper()): GenericJacksonSerde<T> =
    GenericJacksonSerde(mapper, T::class)


class GenericJacksonSerde<T: Any>(
    private val mapper: ObjectMapper = ObjectMapper(),
    private val clazz: KClass<T>
) : Serde<T> {
    override fun serializer(): Serializer<T> =
        Serializer<T> { _, t -> mapper.writeValueAsBytes(t) }

    override fun deserializer(): Deserializer<T> =
        Deserializer { _, bytes -> mapper.readValue(bytes, clazz.java) }

}