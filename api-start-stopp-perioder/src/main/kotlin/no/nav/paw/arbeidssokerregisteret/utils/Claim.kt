package no.nav.paw.arbeidssokerregisteret.utils

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import java.util.*

sealed class Claim<A: Any>(
    val issuer: String,
    val claimName: String,
    val fromString: (String) -> A
)

data object AzureName : Claim<String>("azure", "name", { it })
data object AzureNavIdent : Claim<String>("azure", "NAVident", { it })
data object AzureOID : Claim<UUID>("azure", "oid", UUID::fromString)
data object TokenXPID : Claim<Identitetsnummer>("tokenx", "pid", ::Identitetsnummer)
