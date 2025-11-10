package no.nav.paw.security.authentication.model

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import java.util.*

abstract class Claim<A : Any>(
    open val name: String,
    open val resolve: (Any) -> A
)

sealed class SingleClaim<A : Any>(
    override val name: String,
    override val resolve: (Any) -> A
) : Claim<A>(name, resolve)

sealed class ListClaim<A : Any>(
    override val name: String,
    override val resolve: (Any) -> List<A>
) : Claim<List<A>>(name, resolve)

data object ACR : SingleClaim<String>("acr", { it.toString() })
data object PID : SingleClaim<Identitetsnummer>("pid", { Identitetsnummer(it.toString()) })
data object OID : SingleClaim<UUID>("oid", { UUID.fromString(it.toString()) })
data object Name : SingleClaim<String>("name", { it.toString() })
data object NavIdent : SingleClaim<String>("NAVident", { it.toString() })
data object Roles : ListClaim<String>("roles", { value -> (value as List<*>).map { it.toString() } })

class Claims(private val claims: Map<Claim<*>, Any>) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(claim: Claim<T>): T? = claims[claim] as T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrThrow(claim: Claim<T>): T = claims[claim] as T?
        ?: throw UgyldigBearerTokenException("Bearer Token mangler påkrevd claim ${claim.name}")

    fun isEmpty(): Boolean = claims.isEmpty()

    fun contains(claim: Claim<*>): Boolean = claims.containsKey(claim)
}
