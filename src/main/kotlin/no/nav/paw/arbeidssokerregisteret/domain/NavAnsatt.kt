package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.arbeidssokerregisteret.utils.*
import java.util.*

data class NavAnsatt(
    val azureId: UUID,
    val ident: String
)

fun navAnsatt(claims: ResolvedClaims): NavAnsatt? {
    val azureOid = claims[AzureOID]
    val azureNavIdent = claims[AzureNavIdent]
    return if (azureOid != null && azureNavIdent != null) {
        NavAnsatt(azureId = azureOid, ident = azureNavIdent)
    } else {
        null
    }
}
