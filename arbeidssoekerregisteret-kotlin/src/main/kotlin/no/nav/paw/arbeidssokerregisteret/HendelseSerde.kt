package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssokerregisteret.intern.v1.*

fun serializeToBytes(objectMapper: ObjectMapper, hendelse: Hendelse): ByteArray {
    return objectMapper.writeValueAsBytes(hendelse)
}

fun deserialize(objectMapper: ObjectMapper, json: ByteArray): Hendelse {
    val node = objectMapper.readTree(json)
    return when (val hendelseType = node.get("hendelseType")?.asText()) {
        startetHendelseType -> objectMapper.readValue<Startet>(node.traverse())
        avsluttetHendelseType -> objectMapper.readValue<Avsluttet>(node.traverse())
        opplysningerOmArbeidssoekerHendelseType -> objectMapper.readValue<OpplysningerOmArbeidssoekerMottatt>(node.traverse())
        else -> throw IllegalArgumentException("Ukjent hendelse type: '$hendelseType'")
    }
}