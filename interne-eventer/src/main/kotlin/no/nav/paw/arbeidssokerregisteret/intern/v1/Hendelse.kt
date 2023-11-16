package no.nav.paw.arbeidssokerregisteret.intern.v1

import java.util.*

const val startetHendelseType = "intern.v1.startet"
const val avsluttetHendelseType = "intern.v1.avsluttet"
const val situasjonMottattHendelseType = "intern.v1.situasjon_mottatt"

interface Hendelse: HarIdentitetsnummer, HarMetadata {
    val hendelseId: UUID
    val hendelseType: String
}
