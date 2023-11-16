package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssokerregisteret.intern.v1.*

fun serialize(objectMapper: ObjectMapper, hendelse: Hendelse): String {
    return objectMapper.writeValueAsString(hendelse)
}

fun deserialize(objectMapper: ObjectMapper, json: String): Hendelse {
    val node = objectMapper.readTree(json)
    return when (val hendelseType = hendelseType(node.get("hendelseType")?.asText())) {
        startetHendelseType -> objectMapper.readValue<Startet>(node.traverse())
        avsluttetHendelseType -> objectMapper.readValue<Avsluttet>(node.traverse())
        situasjonMottattHendelseType -> objectMapper.readValue<SituasjonMottatt>(node.traverse())
        else -> throw IllegalArgumentException("Ukjent hendelse type: '$hendelseType'")
    }
}