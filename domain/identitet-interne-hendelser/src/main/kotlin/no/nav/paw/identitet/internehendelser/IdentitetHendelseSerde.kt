package no.nav.paw.identitet.internehendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

private val buildObjectMapper
    get(): ObjectMapper = ObjectMapper()
        .registerKotlinModule()
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
            PAW_IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.readValue<PawIdentiteterEndret>(node.traverse())
            PDL_IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.readValue<PdlIdentiteterEndret>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }

    fun deserializeFromString(json: String): IdentitetHendelse {
        val node = objectMapper.readTree(json)
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            null -> throw IllegalArgumentException("Hendelse mangler type")
            PAW_IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.treeToValue(node, PawIdentiteterEndret::class.java)
            PDL_IDENTITETER_ENDRET_HENDELSE_TYPE -> objectMapper.treeToValue(node, PdlIdentiteterEndret::class.java)
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }
}
