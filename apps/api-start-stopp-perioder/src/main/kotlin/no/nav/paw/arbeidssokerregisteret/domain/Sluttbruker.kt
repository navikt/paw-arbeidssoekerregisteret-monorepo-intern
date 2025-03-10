package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXACR
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID

data class Sluttbruker(
    val identitetsnummer: Identitetsnummer,
    val sikkerhetsnivaa: String?
)

fun sluttbruker(claims: ResolvedClaims): Sluttbruker? {
    val identitetsnummer = claims[TokenXPID]
    val sikkerhetsnivaa = claims[TokenXACR]
    return identitetsnummer?.let { Sluttbruker(it, sikkerhetsnivaa) }
}
