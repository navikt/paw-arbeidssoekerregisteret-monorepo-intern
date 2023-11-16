package no.nav.paw.arbeidssokerregisteret.intern.v1

import java.util.*

interface Hendelse: HarIdentitetsnummer, HarMetadata {
    val hendelseId: UUID
    val hendelseType: HendelseType
}

sealed interface HendelseType {
    val typeIdentifikator: String
}

private data class HendelseTypeImpl(
    override val typeIdentifikator: String
): HendelseType

val startetHendelseType: HendelseType = HendelseTypeImpl("intern.v1.startet")
val avsluttetHendelseType: HendelseType = HendelseTypeImpl("intern.v1.avsluttet")
val situasjonMottattHendelseType: HendelseType = HendelseTypeImpl("intern.v1.situasjon.mottatt")

fun hendelseType(typeIdentifikator: String?): HendelseType =
    when (typeIdentifikator) {
        startetHendelseType.typeIdentifikator -> startetHendelseType
        avsluttetHendelseType.typeIdentifikator -> avsluttetHendelseType
        situasjonMottattHendelseType.typeIdentifikator -> situasjonMottattHendelseType
        else -> throw IllegalArgumentException("Ukjent hendelse type: '$typeIdentifikator'")
    }