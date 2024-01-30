package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.domain.Avvist
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.Uavklart
import no.nav.paw.arbeidssokerregisteret.evaluering.Attributter
import no.nav.paw.arbeidssokerregisteret.evaluering.haandterResultat

fun sjekkOmRettTilRegistrering(evalueringer: Set<Attributter>): Resultat {
    val ikkeRettTilRegistrering = haandterResultat(
        regler = harIkkeRettTilRegistrering,
        resultat = evalueringer
    ) { regelBeskrivelse, evalueringer ->
        Avvist(
            melding = regelBeskrivelse,
            attributter = evalueringer
        )
    }.firstOrNull()
    if (ikkeRettTilRegistrering != null) {
        return ikkeRettTilRegistrering
    } else {
        return haandterResultat(
            regler = harRettTilRegistrering,
            resultat = evalueringer
        ) { regelBeskrivelse, evalueringer ->
            OK(
                melding = regelBeskrivelse,
                attributter = evalueringer
            )
        }.firstOrNull() ?: Uavklart(
            melding = "Ingen regler funnet for evaluering: $evalueringer",
            attributter = evalueringer
        )
    }
}

val harRettTilRegistrering: List<Regel> = listOf(
    "Er registrert av ansatt med tilgang til bruker"(
        Attributter.ANSATT_TILGANG
    ),
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Attributter.ER_OVER_18_AAR,
        Attributter.HAR_NORSK_ADRESSE,
        Attributter.HAR_GYLDIG_OPPHOLDSTILLATELSE
    ),
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Attributter.ER_OVER_18_AAR,
        Attributter.HAR_NORSK_ADRESSE,
        Attributter.BOSATT_ETTER_FREG_LOVEN
    ),
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Attributter.ER_OVER_18_AAR,
        Attributter.HAR_NORSK_ADRESSE,
        Attributter.DNUMMER
    )
)

val harIkkeRettTilRegistrering: List<Regel> = listOf(
    "Er under 18 år"(
        Attributter.ER_UNDER_18_AAR,
        Attributter.IKKE_ANSATT
    ),
    "Bor i utlandet"(
        Attributter.HAR_UTENLANDSK_ADRESSE,
        Attributter.IKKE_ANSATT
    )
)
