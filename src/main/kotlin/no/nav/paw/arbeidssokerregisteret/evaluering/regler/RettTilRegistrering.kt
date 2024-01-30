package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.domain.Avvist
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.Uavklart
import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.haandterResultat

fun sjekkOmRettTilRegistrering(samletFakta: Set<Fakta>): Resultat {
    val ikkeRettTilRegistrering = haandterResultat(
        regler = harIkkeRettTilRegistrering,
        samletFakta = samletFakta
    ) { regelBeskrivelse, evalueringer ->
        Avvist(
            melding = regelBeskrivelse,
            fakta = evalueringer
        )
    }.firstOrNull()
    if (ikkeRettTilRegistrering != null) {
        return ikkeRettTilRegistrering
    } else {
        return haandterResultat(
            regler = harRettTilRegistrering,
            samletFakta = samletFakta
        ) { regelBeskrivelse, evalueringer ->
            OK(
                melding = regelBeskrivelse,
                fakta = evalueringer
            )
        }.firstOrNull() ?: Uavklart(
            melding = "Ingen regler funnet for evaluering: $samletFakta",
            fakta = samletFakta
        )
    }
}

val harRettTilRegistrering: List<Regel> = listOf(
    "Er registrert av ansatt med tilgang til bruker"(
        Fakta.ANSATT_TILGANG
    ),
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.HAR_GYLDIG_OPPHOLDSTILLATELSE
    ),
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.BOSATT_ETTER_FREG_LOVEN
    ),
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.DNUMMER
    )
)

val harIkkeRettTilRegistrering: List<Regel> = listOf(
    "Er under 18 år"(
        Fakta.ER_UNDER_18_AAR,
        Fakta.IKKE_ANSATT
    ),
    "Bor i utlandet"(
        Fakta.HAR_UTENLANDSK_ADRESSE,
        Fakta.IKKE_ANSATT
    )
)
