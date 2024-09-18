package no.nav.paw.bekreftelse.api.authz

import no.nav.paw.bekreftelse.api.model.Identitetsnummer
import java.util.*

sealed class Issuer(
    val name: String
)

data object TokenX : Issuer("tokenx")
data object Azure : Issuer("azure")

sealed class Claim<A : Any>(
    val issuer: Issuer,
    val claimName: String,
    val fromString: (String) -> A
)

data object AzureName : Claim<String>(Azure, "name", { it })
data object AzureNavIdent : Claim<String>(Azure, "NAVident", { it })
data object AzureOID : Claim<UUID>(Azure, "oid", UUID::fromString)
data object TokenXPID : Claim<Identitetsnummer>(TokenX, "pid", ::Identitetsnummer)
