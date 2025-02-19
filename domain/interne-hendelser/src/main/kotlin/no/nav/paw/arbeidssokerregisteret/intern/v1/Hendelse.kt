package no.nav.paw.arbeidssokerregisteret.intern.v1

import java.util.*

const val startetHendelseType = "intern.v1.startet"
const val avsluttetHendelseType = "intern.v1.avsluttet"
const val avvistHendelseType = "intern.v1.avvist"
const val avvistStoppAvPeriodeHendelseType = "intent.v1.avvist_stopp_av_periode"
const val opplysningerOmArbeidssoekerHendelseType = "intern.v1.opplysninger_om_arbeidssoeker"
const val identitetsnummerSammenslaattHendelseType = "intern.v1.identitetsnummer_sammenslaatt"
const val arbeidssoekerIdFlettetInn = "intern.v1.arbeidssoeker_id_flettet_inn"
const val automatiskIdMergeIkkeMulig = "intern.v1.automatisk_id_merge_ikke_mulig"

sealed interface Hendelse : HarIdentitetsnummer, HarMetadata {
    val id: Long
    val hendelseId: UUID
    val hendelseType: String
}
