package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Arbeidserfaring as ApiArbeidserfaring
import no.nav.paw.arbeidssokerregisteret.intern.v1.Arbeidserfaring as InternApiArbeidserfaring

data class Arbeidserfaring(
    val harHattArbeid: JaNeiVetIkke,
)

fun arbeidserfaring(arbeidserfaring: InternApiArbeidserfaring): Arbeidserfaring =
    Arbeidserfaring(
        harHattArbeid = jaNeiVetIkke(arbeidserfaring.harHattArbeid)
    )

fun Arbeidserfaring.api(): ApiArbeidserfaring =
    ApiArbeidserfaring(
        harHattArbeid.api()
    )
