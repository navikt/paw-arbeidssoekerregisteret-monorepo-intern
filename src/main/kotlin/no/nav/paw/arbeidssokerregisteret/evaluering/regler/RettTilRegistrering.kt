package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.domain.Avvist
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.Uavklart
import no.nav.paw.arbeidssokerregisteret.evaluering.Attributt
import no.nav.paw.arbeidssokerregisteret.evaluering.haandterResultat

fun sjekkOmRettTilRegistrering(evalueringer: Set<Attributt>): Resultat {
    val ikkeRettTilRegistrering = haandterResultat(
        regler = harIkkeRettTilRegistrering,
        resultat = evalueringer
    ) { regelBeskrivelse, evalueringer ->
        Avvist(
            melding = regelBeskrivelse,
            attributt = evalueringer
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
                attributt = evalueringer
            )
        }.firstOrNull() ?: Uavklart(
            melding = "Ingen regler funnet for evaluering: $evalueringer",
            attributt = evalueringer
        )
    }
}

val harRettTilRegistrering: List<Regel> = listOf(
    "Er registrert av ansatt med tilgang til bruker"(
        Attributt.ANSATT_TILGANG
    ),
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Attributt.ER_OVER_18_AAR,
        Attributt.HAR_NORSK_ADRESSE,
        Attributt.HAR_GYLDIG_OPPHOLDSTILLATELSE
    ),
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Attributt.ER_OVER_18_AAR,
        Attributt.HAR_NORSK_ADRESSE,
        Attributt.BOSATT_ETTER_FREG_LOVEN
    ),
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Attributt.ER_OVER_18_AAR,
        Attributt.HAR_NORSK_ADRESSE,
        Attributt.DNUMMER
    )
)

val harIkkeRettTilRegistrering: List<Regel> = listOf(
    "Er under 18 år"(
        Attributt.ER_UNDER_18_AAR,
        Attributt.IKKE_ANSATT
    ),
    "Bor i utlandet"(
        Attributt.HAR_UTENLANDSK_ADRESSE,
        Attributt.IKKE_ANSATT
    )
)
