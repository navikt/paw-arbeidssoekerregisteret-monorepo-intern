package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun AutorisasjonService.evalNavAnsattTilgang(identitetsnummer: Identitetsnummer): Attributt {
    val navAnsatt = navAnsatt(claims)
    return if (navAnsatt != null) {
        if (verifiserVeilederTilgangTilBruker(navAnsatt, identitetsnummer)) {
            Attributt.ANSATT_TILGANG
        } else {
            Attributt.ANSATT_IKKE_TILGANG
        }
    } else {
        Attributt.IKKE_ANSATT
    }
}
