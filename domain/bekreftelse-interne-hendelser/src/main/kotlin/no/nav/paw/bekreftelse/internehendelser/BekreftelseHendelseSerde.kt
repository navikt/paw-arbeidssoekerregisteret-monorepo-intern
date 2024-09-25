package no.nav.paw.bekreftelse.internehendelser

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

class BekreftelseHendelseSerde: Serde<BekreftelseHendelse> {
    override fun serializer(): Serializer<BekreftelseHendelse> {
        return BekreftelseHendelseSerializer
    }

    override fun deserializer(): Deserializer<BekreftelseHendelse> {
        return BekreftelseHendelseDeserializer
    }
}

object BekreftelseHendelseSerializer: Serializer<BekreftelseHendelse> {
    override fun serialize(topic: String?, data: BekreftelseHendelse?): ByteArray? {
        return data?.let { objectMapper.writeValueAsBytes(it) }
    }
}

object BekreftelseHendelseDeserializer: Deserializer<BekreftelseHendelse> {
    override fun deserialize(topic: String?, data: ByteArray?): BekreftelseHendelse {
        val node = objectMapper.readTree(data)
        return when (val hendelseType = node.get("hendelseType")?.asText()) {
            leveringsfristUtloeptHendelseType -> objectMapper.readValue<LeveringsfristUtloept>(node.traverse())
            eksternGracePeriodeUtloeptHendelseType -> objectMapper.readValue<EksternGracePeriodeUtloept>(node.traverse())
            registerGracePeriodeUtloeptHendelseType -> objectMapper.readValue<RegisterGracePeriodeUtloept>(node.traverse())
            bekreftelseTilgjengeligHendelseType -> objectMapper.readValue<BekreftelseTilgjengelig>(node.traverse())
            meldingMottattHendelseType -> objectMapper.readValue<BekreftelseMeldingMottatt>(node.traverse())
            periodeAvsluttetHendelsesType -> objectMapper.readValue<PeriodeAvsluttet>(node.traverse())
            registerGracePeriodeGjenstaaendeTid -> objectMapper.readValue<RegisterGracePeriodeGjendstaaendeTid>(node.traverse())
            else -> throw IllegalArgumentException("Ukjent hendelseType: $hendelseType")
        }
    }
}