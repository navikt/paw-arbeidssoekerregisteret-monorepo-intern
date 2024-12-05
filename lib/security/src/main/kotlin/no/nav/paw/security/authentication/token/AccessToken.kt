package no.nav.paw.security.authentication.token

import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import java.util.*

data class AccessToken(
    val jwt: String,
    val issuer: Issuer,
    val claims: Claims
) {
    fun isM2MToken(): Boolean {
        return claims.getOrNull(Roles)?.contains("access_as_application") ?: false
    }
}

sealed class Issuer(val name: String)

data object IdPorten : Issuer("idporten")
data object TokenX : Issuer("tokenx")
data object AzureAd : Issuer("azure")

class Claims(private val claims: Map<Claim<*>, Any>) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(claim: Claim<T>): T? = claims[claim] as T?

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrThrow(claim: Claim<T>): T = claims[claim] as T?
        ?: throw UgyldigBearerTokenException("Bearer Token mangler påkrevd claim ${claim.name}")

    fun isEmpty(): Boolean = claims.isEmpty()

    fun contains(claim: Claim<*>): Boolean = claims.containsKey(claim)
}

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

data object PID : SingleClaim<Identitetsnummer>("pid", { Identitetsnummer(it.toString()) })
data object OID : SingleClaim<UUID>("oid", { UUID.fromString(it.toString()) })
data object Name : SingleClaim<String>("name", { it.toString() })
data object NavIdent : SingleClaim<String>("NAVident", { it.toString() })
data object Roles : ListClaim<String>("roles", { value -> (value as List<*>).map { it.toString() } })

sealed class ResolveToken(val issuer: Issuer, val claims: List<Claim<*>>)

data object IdPortenToken : ResolveToken(IdPorten, listOf(PID))
data object TokenXToken : ResolveToken(TokenX, listOf(PID))
data object AzureAdToken : ResolveToken(AzureAd, listOf(OID, Name, NavIdent, Roles))

private val validTokens: List<ResolveToken> = listOf(IdPortenToken, TokenXToken, AzureAdToken)

fun TokenValidationContext.resolveTokens(): List<AccessToken> {
    return validTokens
        .mapNotNull { resolveToken ->
            getJwtToken(resolveToken.issuer.name)?.let { resolveToken to it }
        }
        .map { (resolveToken, jwtToken) ->
            val claims = jwtToken.resolveClaims(resolveToken)
            AccessToken(jwtToken.encodedToken, resolveToken.issuer, claims)
        }
}

private fun JwtToken.resolveClaims(resolveToken: ResolveToken): Claims {
    val claims = resolveToken.claims
        .mapNotNull { claim ->
            when (claim) {
                is ListClaim<*> -> {
                    val value = jwtTokenClaims.getAsList(claim.name)
                    value?.let { claim to claim.resolve(value) }
                }

                else -> {
                    val value = jwtTokenClaims.getStringClaim(claim.name)
                    value?.let { claim to claim.resolve(value) }
                }
            }
        }
        .toMap()
    return Claims(claims)
}
