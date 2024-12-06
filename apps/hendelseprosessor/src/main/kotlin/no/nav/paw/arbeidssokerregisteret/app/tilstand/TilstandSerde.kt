package no.nav.paw.arbeidssokerregisteret.app.tilstand

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class TilstandSerde : Serde<TilstandV1> {
    private val objectMapper = ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
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
    override fun serializer() = TilstandSerializer(objectMapper)
    override fun deserializer() = TilstandDeserializer(objectMapper)
}

class TilstandSerializer(private val objectMapper: ObjectMapper): Serializer<TilstandV1> {
    override fun serialize(topic: String?, data: TilstandV1?): ByteArray {
        return objectMapper.writeValueAsBytes(data)
    }
}

class TilstandDeserializer(private val objectMapper: ObjectMapper): Deserializer<TilstandV1> {
    override fun deserialize(topic: String?, data: ByteArray?): TilstandV1? {
        if (data == null) return null
        val node = objectMapper.readTree(data)
        return when (val classVersion = node.get("classVersion")?.asText()) {
            TilstandV1.classVersion -> objectMapper.readValue<TilstandV1>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent version av intern tilstandsklasse: '$classVersion', bytes=${data.size}")
        }
    }
}
