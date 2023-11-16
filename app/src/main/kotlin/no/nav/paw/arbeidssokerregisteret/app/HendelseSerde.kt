package no.nav.paw.arbeidssokerregisteret.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.serializeToBytes
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class HendelseSerde : Serde<Hendelse> {
    private val objectMapper = ObjectMapper()
        .registerModules(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
            com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()
        )
    override fun serializer() = HendelseSerializer(objectMapper)
    override fun deserializer() = HendelseDeserializer(objectMapper)
}

class HendelseSerializer(private val objectMapper: ObjectMapper): Serializer<Hendelse> {
    override fun serialize(topic: String?, data: Hendelse?): ByteArray {
        return data?.let {
            serializeToBytes(objectMapper, it)
        } ?: ByteArray(0)
    }
}

class HendelseDeserializer(private val objectMapper: ObjectMapper): Deserializer<Hendelse> {
    override fun deserialize(topic: String?, data: ByteArray?): Hendelse? {
        if (data == null) return null
        return no.nav.paw.arbeidssokerregisteret.deserialize(objectMapper, data)
    }
}
