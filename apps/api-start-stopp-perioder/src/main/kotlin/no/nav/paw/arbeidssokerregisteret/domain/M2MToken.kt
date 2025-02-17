package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.arbeidssokerregisteret.utils.AzureAzpName
import no.nav.paw.arbeidssokerregisteret.utils.AzureRoles
import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims

data class M2MToken(val tjeneste: String)

fun m2mToken(claims: ResolvedClaims): M2MToken? {
    val roles = claims[AzureRoles]
    val azp = claims[AzureAzpName]
    return if (roles != null && azp != null && roles.contains("access_as_application")) {
        M2MToken(azp)
    } else {
        null
    }
}
