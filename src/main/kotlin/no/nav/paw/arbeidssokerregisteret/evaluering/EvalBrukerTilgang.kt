package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

context(RequestScope)
fun evalBrukerTilgang(identitetsnummer: Identitetsnummer): Attributter {
    return claims[TokenXPID]?.let { authenticatedUser ->
        if (authenticatedUser != identitetsnummer) {
            Attributter.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        } else {
            Attributter.SAMME_SOM_INNLOGGET_BRUKER
        }
    } ?: Attributter.TOKENX_PID_IKKE_FUNNET
}
