package no.nav.paw.arbeidssokerregisteret.app.tilstand

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.cfg.DateTimeFeature

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.treeToValue
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class TilstandSerde : Serde<TilstandV1> {
    private val objectMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // Behold Jackson 2-formatet i state store: tidspunkter som tall
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .addModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
        .build()
    override fun serializer() = TilstandSerializer(objectMapper)
    override fun deserializer() = TilstandDeserializer(objectMapper)
}

class TilstandSerializer(private val objectMapper: ObjectMapper): Serializer<TilstandV1> {
    override fun serialize(topic: String?, data: TilstandV1?): ByteArray? {
        return if (data == null) {
            null
        } else {
            objectMapper.writeValueAsBytes(data)
        }
    }
}

class TilstandDeserializer(private val objectMapper: ObjectMapper): Deserializer<TilstandV1> {
    override fun deserialize(topic: String?, data: ByteArray?): TilstandV1? {
        if (data == null || String(data).equals("null", ignoreCase = true)) {
            return null
        }
        val node = objectMapper.readTree(data)
        return when (val classVersion = node.get("classVersion")?.asText()) {
            TilstandV1.classVersion -> objectMapper.treeToValue<TilstandV1>(node)
            else -> throw IllegalArgumentException("Ukjent version av intern tilstandsklasse: '$classVersion', bytes=${data.size}")
        }
    }
}
