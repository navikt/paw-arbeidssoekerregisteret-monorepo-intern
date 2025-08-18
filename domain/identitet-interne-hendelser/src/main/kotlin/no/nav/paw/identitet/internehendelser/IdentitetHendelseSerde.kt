package no.nav.paw.identitet.internehendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

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
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.readValue<IdentiteterEndretHendelse>(node.traverse())
            IDENTITETER_MERGET_HENDELSE_TYPE -> objectMapper.readValue<IdentiteterMergetHendelse>(node.traverse())
            IDENTITETER_SPLITTET_HENDELSE_TYPE -> objectMapper.readValue<IdentiteterSplittetHendelse>(node.traverse())
            IDENTITETER_SLETTET_HENDELSE_TYPE -> objectMapper.readValue<IdentiteterSlettetHendelse>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }

    fun deserializeFromString(json: String): IdentitetHendelse {
        val node = objectMapper.readTree(json)
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            null -> throw IllegalArgumentException("Hendelse mangler type")
            IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.treeToValue(node, IdentiteterEndretHendelse::class.java)
            IDENTITETER_MERGET_HENDELSE_TYPE -> objectMapper.treeToValue(node, IdentiteterMergetHendelse::class.java)
            IDENTITETER_SPLITTET_HENDELSE_TYPE -> objectMapper.treeToValue(node, IdentiteterSplittetHendelse::class.java)
            IDENTITETER_SLETTET_HENDELSE_TYPE -> objectMapper.treeToValue(node, IdentiteterSlettetHendelse::class.java)
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }
}
