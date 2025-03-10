package no.nav.paw.arbeidssokerregisteret.utils

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import java.util.*

abstract class Claim<A : Any>(
    open val issuer: String,
    open val claimName: String,
    open val resolve: (Any) -> A
)

sealed class SingeClaim<A : Any>(
    override val issuer: String,
    override val claimName: String,
    override val resolve: (Any) -> A
) : Claim<A>(issuer, claimName, resolve)

sealed class ListClaim<A : Any>(
    override val issuer: String,
    override val claimName: String,
    override val resolve: (Any) -> List<A>
) : Claim<List<A>>(issuer, claimName, resolve)

data object AzureName : SingeClaim<String>("azure", "name", Any::toString)
data object AzureNavIdent : SingeClaim<String>("azure", "NAVident", Any::toString)
data object AzureOID : SingeClaim<UUID>("azure", "oid", Any::asUUID)
data object AzureRoles : ListClaim<String>("azure", "roles", Any::asListOfStrings)
data object AzureAzpName : SingeClaim<String>("azure", "azp_name", Any::toString)
data object TokenXPID : SingeClaim<Identitetsnummer>("tokenx", "pid", Any::asIdentitetsnummer)
data object TokenXACR : SingeClaim<String>("tokenx", "acr", Any::toString)

private fun Any.asUUID(): UUID = UUID.fromString(this.toString())
private fun Any.asIdentitetsnummer(): Identitetsnummer = Identitetsnummer(this.toString())
private fun Any.asListOfStrings(): List<String> = (this as List<*>).map { it.toString() }
