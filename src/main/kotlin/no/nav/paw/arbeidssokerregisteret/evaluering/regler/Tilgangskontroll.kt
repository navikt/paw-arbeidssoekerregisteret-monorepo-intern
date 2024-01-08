package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.evaluering.Evaluation
import no.nav.paw.arbeidssokerregisteret.evaluering.evalBrukerTilgang
import no.nav.paw.arbeidssokerregisteret.evaluering.evalNavAnsattTilgang
import no.nav.paw.arbeidssokerregisteret.evaluering.haandterResultat
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun genererTilgangsResultat(
    autorisasjonService: AutorisasjonService,
    identitetsnummer: Identitetsnummer
): TilgangskontrollResultat =
    evalTilgang(autorisasjonService, identitetsnummer)
    .let(::genererTilgangsResultat)
context(RequestScope)
fun evalTilgang(autorisasjonService: AutorisasjonService, identitetsnummer: Identitetsnummer): Set<Evaluation> {
    val ansattEvaluation = autorisasjonService.evalNavAnsattTilgang(identitetsnummer)
    val brukerEvaluation = evalBrukerTilgang(identitetsnummer)
    return setOf(ansattEvaluation, brukerEvaluation)
}

fun genererTilgangsResultat(
    tilgangsEvalueringResultat: Set<Evaluation>,
): TilgangskontrollResultat {
    val nektTilgang = haandterResultat(
        regler = ikkeTilgang,
        resultat = tilgangsEvalueringResultat
    ) { regelBeskrivelse, evalueringer ->
        IkkeTilgang(
            melding = regelBeskrivelse,
            evaluation = evalueringer
        )
    }.firstOrNull()
    if (nektTilgang != null) {
        return nektTilgang
    } else {
        return haandterResultat(
            regler = tilgang,
            resultat = tilgangsEvalueringResultat
        ) { regelBeskrivelse, evalueringer ->
            OK(
                melding = regelBeskrivelse,
                evaluation = evalueringer
            )
        }.firstOrNull() ?: IkkeTilgang(
            melding = "Ingen regler funnet for evaluering: $tilgangsEvalueringResultat",
            evaluation = tilgangsEvalueringResultat
        )
    }
}


val ikkeTilgang: Map<String, List<Evaluation>> = mapOf(
    "Ansatt har ikke tilgang til bruker" to listOf(
        Evaluation.ANSATT_IKKE_TILGANG
    ),
    "Bruker prøver å endre for annen bruker" to listOf(
        Evaluation.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
        Evaluation.IKKE_ANSATT
    ),
)

val tilgang: Map<String, List<Evaluation>> = mapOf(
    "Ansatt har tilgang til bruker" to listOf(
        Evaluation.ANSATT_TILGANG
    ),
    "Bruker prøver å endre for seg selv" to listOf(
        Evaluation.SAMME_SOM_INNLOGGET_BRUKER,
        Evaluation.IKKE_ANSATT
    )
)
