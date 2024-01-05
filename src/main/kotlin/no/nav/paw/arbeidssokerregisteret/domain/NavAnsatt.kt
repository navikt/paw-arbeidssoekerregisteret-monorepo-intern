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
    val azureName = claims[AzureName]
    return if (azureOid != null && azureNavIdent != null && azureName != null) {
        NavAnsatt(azureId = azureOid, ident = azureName)
    } else {
        null
    }
}
