package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeSammeSomInnloggerBruker
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SammeSomInnloggetBruker
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.TokenXPidIkkeFunnet
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.paw.felles.model.Identitetsnummer

fun RequestScope.sluttbrukerTilgangFakta(identitetsnummer: Identitetsnummer): Opplysning {
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

