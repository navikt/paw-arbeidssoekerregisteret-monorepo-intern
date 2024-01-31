package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.Fakta
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun AutorisasjonService.navAnsattTilgangFakta(identitetsnummer: Identitetsnummer): Fakta {
    val navAnsatt = navAnsatt(claims)
    return if (navAnsatt != null) {
        if (verifiserVeilederTilgangTilBruker(navAnsatt, identitetsnummer)) {
            Fakta.ANSATT_TILGANG
        } else {
            Fakta.ANSATT_IKKE_TILGANG
        }
    } else {
        Fakta.IKKE_ANSATT
    }
}
