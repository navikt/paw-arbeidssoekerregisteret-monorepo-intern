package no.nav.paw.arbeidssokerregisteret.evaluering.regler.tilgangskontroll

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.evalBrukerTilgang
import no.nav.paw.arbeidssokerregisteret.evaluering.evalNavAnsattTilgang
import no.nav.paw.arbeidssokerregisteret.evaluering.plus
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.evaluer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun genererTilgangsResultat(
    autorisasjonService: AutorisasjonService,
    identitetsnummer: Identitetsnummer
): TilgangskontrollResultat {
    val ansattEvaluation = autorisasjonService.evalNavAnsattTilgang(identitetsnummer)
    val brukerEvaluation = evalBrukerTilgang(identitetsnummer)
    return (ansattEvaluation + brukerEvaluation)
        .let(::genererTilgangsResultat)
}

fun genererTilgangsResultat(
    autentisaeringsFakta: Set<Fakta>,
): TilgangskontrollResultat {
    return tilgang
        .filter { regel -> regel.evaluer(autentisaeringsFakta) }
        .map { (regelBeskrivelse, _) ->
            OK(
                melding = regelBeskrivelse,
                fakta = autentisaeringsFakta
            )
        }.firstOrNull() ?: IkkeTilgang(
        melding = "Oppfyller ikke kriteriende i standard tilgangskontroll",
        fakta = autentisaeringsFakta
    )
}
