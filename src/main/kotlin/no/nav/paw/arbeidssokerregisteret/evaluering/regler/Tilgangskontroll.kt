package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.evaluering.Attributt
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
fun evalTilgang(autorisasjonService: AutorisasjonService, identitetsnummer: Identitetsnummer): Set<Attributt> {
    val ansattEvaluation = autorisasjonService.evalNavAnsattTilgang(identitetsnummer)
    val brukerEvaluation = evalBrukerTilgang(identitetsnummer)
    return setOf(ansattEvaluation, brukerEvaluation)
}

fun genererTilgangsResultat(
    tilgangsEvalueringResultat: Set<Attributt>,
): TilgangskontrollResultat {
    val nektTilgang = haandterResultat(
        regler = ikkeTilgang,
        resultat = tilgangsEvalueringResultat
    ) { regelBeskrivelse, evalueringer ->
        IkkeTilgang(
            melding = regelBeskrivelse,
            attributt = evalueringer
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
                attributt = evalueringer
            )
        }.firstOrNull() ?: IkkeTilgang(
            melding = "Ingen regler funnet for evaluering: $tilgangsEvalueringResultat",
            attributt = tilgangsEvalueringResultat
        )
    }
}


val ikkeTilgang: List<Regel> = listOf(
    "Ansatt har ikke tilgang til bruker"(
        Attributt.ANSATT_IKKE_TILGANG
    ),
    "Bruker prøver å endre for annen bruker"(
        Attributt.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
        Attributt.IKKE_ANSATT
    ),
)

val tilgang: List<Regel> = listOf(
    "Ansatt har tilgang til bruker"(
        Attributt.ANSATT_TILGANG
    ),
    "Bruker prøver å endre for seg selv"(
        Attributt.SAMME_SOM_INNLOGGET_BRUKER,
        Attributt.IKKE_ANSATT
    )
)
