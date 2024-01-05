package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun AutorisasjonService.evalNavAnsattTilgang(identitetsnummer: Identitetsnummer): Evaluation {
    val navAnsatt = navAnsatt(claims)
    return if (navAnsatt != null) {
        if (verifiserVeilederTilgangTilBruker(navAnsatt, identitetsnummer)) {
            Evaluation.ANSATT_TILGANG
        } else {
            Evaluation.ANSATT_IKKE_TILGANG
        }
    } else {
        Evaluation.IKKE_ANSATT
    }
}
