package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.domain.Avvist
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.Uavklart
import no.nav.paw.arbeidssokerregisteret.evaluering.Evaluation
import no.nav.paw.arbeidssokerregisteret.evaluering.haandterResultat

fun sjekkOmRettTilRegistrering(evalueringer: Set<Evaluation>): Resultat {
    val ikkeRettTilRegistrering = haandterResultat(
        regler = harIkkeRettTilRegistrering,
        resultat = evalueringer
    ) { regelBeskrivelse, evalueringer ->
        Avvist(
            melding = regelBeskrivelse,
            evaluation = evalueringer
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
                evaluation = evalueringer
            )
        }.firstOrNull() ?: Uavklart(
            melding = "Ingen regler funnet for evaluering: $evalueringer",
            evaluation = evalueringer
        )
    }
}

val harRettTilRegistrering: Map<String, List<Evaluation>> = mapOf(
    "Er registrert av ansatt med tilgang til bruker" to listOf(
        Evaluation.ANSATT_TILGANG
    ),
    "Er over 18 år, har norsk adresse og oppholdstillatelse" to listOf(
        Evaluation.ER_OVER_18_AAR,
        Evaluation.HAR_NORSK_ADRESSE,
        Evaluation.HAR_GYLDIG_OPPHOLDSTILLATELSE
    ),
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven" to listOf(
        Evaluation.ER_OVER_18_AAR,
        Evaluation.HAR_NORSK_ADRESSE,
        Evaluation.BOSATT_ETTER_FREG_LOVEN
    ),
    "Er over 18 år, har norsk adresse og har d-nummer" to listOf(
        Evaluation.ER_OVER_18_AAR,
        Evaluation.HAR_NORSK_ADRESSE,
        Evaluation.DNUMMER
    )
)

val harIkkeRettTilRegistrering: Map<String, List<Evaluation>> = mapOf(
    "Er under 18 år" to listOf(
        Evaluation.ER_UNDER_18_AAR,
        Evaluation.IKKE_ANSATT
    ),
    "Bor i utlandet" to listOf(
        Evaluation.HAR_UTENLANDSK_ADRESSE,
        Evaluation.IKKE_ANSATT
    )
)
