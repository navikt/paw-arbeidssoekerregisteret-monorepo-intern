package no.nav.paw.arbeidssokerregisteret.application.fakta

import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

context(RequestScope)
fun tokenXPidFakta(identitetsnummer: Identitetsnummer): Opplysning {
    return (claims[TokenXPID]?.let { authenticatedUser ->
        if (authenticatedUser != identitetsnummer) {
            Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        } else {
            Opplysning.SAMME_SOM_INNLOGGET_BRUKER
        }
    } ?: Opplysning.TOKENX_PID_IKKE_FUNNET)
        .also { opplysning ->
            Span.current()
                .setAttribute("paw_tokenx_pid", opplysning.toString())
        }
}
