package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

context(RequestScope)
fun AutorisasjonService.navAnsattTilgangFakta(identitetsnummer: Identitetsnummer): Opplysning {
    val navAnsatt = navAnsatt(claims)
    return if (navAnsatt != null) {
        if (verifiserVeilederTilgangTilBruker(navAnsatt, identitetsnummer)) {
            Opplysning.ANSATT_TILGANG
        } else {
            Opplysning.ANSATT_IKKE_TILGANG
        }
    }
    else {
        Opplysning.IKKE_ANSATT
    }
}
