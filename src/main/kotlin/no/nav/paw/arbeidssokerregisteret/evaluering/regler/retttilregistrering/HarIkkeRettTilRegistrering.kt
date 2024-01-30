package no.nav.paw.arbeidssokerregisteret.evaluering.regler.retttilregistrering

import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.Regel
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.invoke

val harIkkeRettTilRegistrering: List<Regel> = listOf(
    "Er under 18 år"(
        Fakta.ER_UNDER_18_AAR
    ),
    "Bor i utlandet"(
        Fakta.HAR_UTENLANDSK_ADRESSE
    )
)
