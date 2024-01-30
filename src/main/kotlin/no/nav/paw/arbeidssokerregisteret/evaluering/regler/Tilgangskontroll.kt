package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
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
fun evalTilgang(autorisasjonService: AutorisasjonService, identitetsnummer: Identitetsnummer): Set<Fakta> {
    val ansattEvaluation = autorisasjonService.evalNavAnsattTilgang(identitetsnummer)
    val brukerEvaluation = evalBrukerTilgang(identitetsnummer)
    return setOf(ansattEvaluation, brukerEvaluation)
}

fun genererTilgangsResultat(
    tilgangsEvalueringResultat: Set<Fakta>,
): TilgangskontrollResultat {
    val nektTilgang = ikkeTilgang
        .filter { regel -> regel.evaluer(tilgangsEvalueringResultat) }
        .map { (regelBeskrivelse, _) ->
            IkkeTilgang(
                melding = regelBeskrivelse,
                fakta = tilgangsEvalueringResultat
            )
        }.firstOrNull()
    if (nektTilgang != null) {
        return nektTilgang
    } else {
        return tilgang
            .filter { regel -> regel.evaluer(tilgangsEvalueringResultat) }
            .map { (regelBeskrivelse, _) ->
                OK(
                    melding = regelBeskrivelse,
                    fakta = tilgangsEvalueringResultat
                )
            }.firstOrNull() ?: IkkeTilgang(
            melding = "Ingen regler funnet for evaluering: $tilgangsEvalueringResultat",
            fakta = tilgangsEvalueringResultat
        )
    }
}

val ikkeTilgang: List<Regel> = listOf(
    "Ansatt har ikke tilgang til bruker"(
        Fakta.ANSATT_IKKE_TILGANG
    ),
    "Bruker prøver å endre for annen bruker"(
        Fakta.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
        Fakta.IKKE_ANSATT
    ),
)

val tilgang: List<Regel> = listOf(
    "Ansatt har tilgang til bruker"(
        Fakta.ANSATT_TILGANG
    ),
    "Bruker prøver å endre for seg selv"(
        Fakta.SAMME_SOM_INNLOGGET_BRUKER,
        Fakta.IKKE_ANSATT
    )
)
