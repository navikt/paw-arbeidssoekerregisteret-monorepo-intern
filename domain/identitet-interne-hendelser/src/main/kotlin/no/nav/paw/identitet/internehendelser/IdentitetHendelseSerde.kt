package no.nav.paw.identitet.internehendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import kotlin.reflect.KClass

private val buildObjectMapper
    get(): ObjectMapper = jacksonObjectMapper()
        .registerModules(JavaTimeModule())

class IdentitetHendelseSerde(
    private val objectMapper: ObjectMapper = buildObjectMapper
) : Serde<IdentitetHendelse> {
    override fun serializer(): Serializer<IdentitetHendelse> {
        return IdentitetHendelseSerializer(objectMapper)
    }

    override fun deserializer(): Deserializer<IdentitetHendelse> {
        return IdentitetHendelseDeserializer(objectMapper)
    }
}

class IdentitetHendelseSerializer(
    private val objectMapper: ObjectMapper = buildObjectMapper
) : Serializer<IdentitetHendelse> {
    override fun serialize(topic: String?, data: IdentitetHendelse?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }

    fun serializeToString(data: IdentitetHendelse): String = objectMapper.writeValueAsString(data)
}

class IdentitetHendelseDeserializer(
    private val objectMapper: ObjectMapper = buildObjectMapper
) : Deserializer<IdentitetHendelse> {
    override fun deserialize(topic: String?, data: ByteArray?): IdentitetHendelse {
        val node = objectMapper.readTree(data)
        val hendelseClass = node.asHendelseClass()
        return objectMapper.treeToValue(node, hendelseClass.java)
    }

    fun deserializeFromString(json: String): IdentitetHendelse {
        val node = objectMapper.readTree(json)
        val hendelseClass = node.asHendelseClass()
        return objectMapper.treeToValue(node, hendelseClass.java)
    }

    private fun JsonNode.asHendelseClass(): KClass<out IdentitetHendelse> {
        return when (val hendelseType = get("hendelseType")?.asText()) {
            IDENTITETER_ENDRET_V1_HENDELSE_TYPE -> IdentiteterEndretHendelse::class
            IDENTITETER_MERGET_V1_HENDELSE_TYPE -> IdentiteterMergetHendelse::class
            IDENTITETER_SPLITTET_V1_HENDELSE_TYPE -> IdentiteterSplittetHendelse::class
            IDENTITETER_SLETTET_V1_HENDELSE_TYPE -> IdentiteterSlettetHendelse::class
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }
}
