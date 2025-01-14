package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

suspend fun navAnsattTilgangFakta(
    autorisasjonService: AutorisasjonService,
    requestScope: RequestScope,
    identitetsnummer: Identitetsnummer
): Opplysning {
    val navAnsatt = navAnsatt(requestScope.claims)
    return (if (navAnsatt != null) {
        if (autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, identitetsnummer)) {
            AnsattTilgang
        } else {
            AnsattIkkeTilgang
        }
    } else {
        IkkeAnsatt
    }).also { opplysning ->
        Span.current()
            .setAttribute("paw_nav_ansatt_tilgang", opplysning.toString())
    }
}
