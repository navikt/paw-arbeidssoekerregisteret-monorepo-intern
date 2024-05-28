package no.nav.paw.rapportering.internehendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

private val objectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule().
    registerModules(JavaTimeModule())

class RapporteringsHendelseSerde: Serde<RapporteringsHendelse> {
    override fun serializer(): Serializer<RapporteringsHendelse> {
        return RapporteringsHendelseSerializer
    }

    override fun deserializer(): Deserializer<RapporteringsHendelse> {
        return RapporteringsHendelseDeserializer
    }
}

object RapporteringsHendelseSerializer: Serializer<RapporteringsHendelse> {
    override fun serialize(topic: String?, data: RapporteringsHendelse?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

object RapporteringsHendelseDeserializer: Deserializer<RapporteringsHendelse> {
    override fun deserialize(topic: String?, data: ByteArray?): RapporteringsHendelse {
        val node = objectMapper.readTree(data)
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            leveringsfristUtloeptHendelseType -> objectMapper.readValue<LeveringsfristUtloept>(node.traverse())
            eksternGracePeriodeUtloeptHendelseType -> objectMapper.readValue<EksternGracePeriodeUtloept>(node.traverse())
            registerGracePeriodeUtloeptHendelseType -> objectMapper.readValue<RegisterGracePeriodeUtloept>(node.traverse())
            rapporteringTilgjengeligHendelseType -> objectMapper.readValue<RapporteringTilgjengelig>(node.traverse())
            meldingMottattHendelseType -> objectMapper.readValue<RapporteringsMeldingMottatt>(node.traverse())
            periodeAvsluttetHendelsesType -> objectMapper.readValue<PeriodeAvsluttet>(node.traverse())
            registerGracePeriodeGjenstaandeTid -> objectMapper.readValue<RegisterGracePeriodeGjendstaaendeTid>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }
}