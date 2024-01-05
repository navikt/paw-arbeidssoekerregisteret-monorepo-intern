package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

context(RequestScope)
fun evalBrukerTilgang(identitetsnummer: Identitetsnummer): Evaluation {
    return claims[TokenXPID]?.let { authenticatedUser ->
        if (authenticatedUser != identitetsnummer) {
            Evaluation.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        } else {
            Evaluation.SAMME_SOM_INNLOGGER_BRUKER
        }
    } ?: Evaluation.TOKENX_PID_IKKE_FUNNET
}
