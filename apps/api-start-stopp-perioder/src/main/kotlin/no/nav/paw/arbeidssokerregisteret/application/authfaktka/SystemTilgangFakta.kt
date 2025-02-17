package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeSystem
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SystemIkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SystemTilgang
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.m2mToken
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService

fun systemTilgangFakta(
    autorisasjonService: AutorisasjonService,
    requestScope: RequestScope
): Opplysning {
    val m2MToken = m2mToken(requestScope.claims)
    return (if (m2MToken != null) {
        if (autorisasjonService.verifiserSystemTilgangTilBruker(m2MToken)) {
            SystemTilgang
        } else {
            SystemIkkeTilgang
        }
    } else {
        IkkeSystem
    }).also { opplysning ->
        Span.current()
            .setAttribute("paw_m2m_token_tilgang", opplysning.toString())
    }
}
