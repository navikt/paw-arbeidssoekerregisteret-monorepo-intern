package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

fun RequestScope.tokenXPidFakta(identitetsnummer: Identitetsnummer): Opplysning {
    return (claims[TokenXPID]?.let { authenticatedUser ->
        if (authenticatedUser != identitetsnummer) {
            IkkeSammeSomInnloggerBruker
        } else {
            SammeSomInnloggetBruker
        }
    } ?: TokenXPidIkkeFunnet)
        .also { opplysning ->
            Span.current()
                .setAttribute("paw_tokenx_pid", opplysning.toString())
        }
}

