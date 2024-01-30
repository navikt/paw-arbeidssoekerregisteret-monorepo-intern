package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

context(RequestScope)
fun evalBrukerTilgang(identitetsnummer: Identitetsnummer): Attributt {
    return claims[TokenXPID]?.let { authenticatedUser ->
        if (authenticatedUser != identitetsnummer) {
            Attributt.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        } else {
            Attributt.SAMME_SOM_INNLOGGET_BRUKER
        }
    } ?: Attributt.TOKENX_PID_IKKE_FUNNET
}
