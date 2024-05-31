package no.nav.paw.arbeidssokerregisteret.intern.v1

import java.util.*

const val startetHendelseType = "intern.v1.startet"
const val avsluttetHendelseType = "intern.v1.avsluttet"
const val avvistHendelseType = "intern.v1.avvist"
const val avvistStoppAvPeriodeHendelseType = "intent.v1.avvist_stopp_av_periode"
const val opplysningerOmArbeidssoekerHendelseType = "intern.v1.opplysninger_om_arbeidssoeker"

sealed interface Hendelse : HarIdentitetsnummer, HarMetadata {
    val id: Long
    val hendelseId: UUID
    val hendelseType: String
}
