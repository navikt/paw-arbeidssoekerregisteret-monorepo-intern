package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

data class Sluttbruker(val identitetsnummer: Identitetsnummer)

fun sluttbruker(claims: ResolvedClaims): Sluttbruker? {
    val identitetsnummer = claims[TokenXPID]
    return identitetsnummer?.let { Sluttbruker(it) }
}
