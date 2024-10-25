package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.api.exception.UgyldigBearerTokenException
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import java.util.*

data class AccessToken(
    val jwt: String,
    val issuer: Issuer,
    val claims: Claims
)

sealed class Issuer(val name: String)

data object IdPorten : Issuer("idporten")
data object TokenX : Issuer("tokenx")
data object Azure : Issuer("azure")

class Claims(private val claims: Map<Claim<*>, Any>) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(claim: Claim<T>): T =
        claims[claim] as T?
            ?: throw UgyldigBearerTokenException("Bearer Token mangler påkrevd claim ${claim.name}")

    fun isEmpty(): Boolean = claims.isEmpty()
}

sealed class Claim<A : Any>(
    val name: String,
    val resolve: (String) -> A
)

data object PID : Claim<Identitetsnummer>("pid", ::Identitetsnummer)
data object OID : Claim<UUID>("oid", UUID::fromString)
data object Name : Claim<String>("name", { it })
data object NavIdent : Claim<String>("NAVident", { it })

sealed class ResolveToken(val issuer: Issuer, val claims: List<Claim<*>>)

data object IdPortenToken : ResolveToken(IdPorten, listOf(PID))
data object TokenXToken : ResolveToken(TokenX, listOf(PID))
data object AzureToken : ResolveToken(Azure, listOf(OID, Name, NavIdent))

private val validTokens: List<ResolveToken> = listOf(IdPortenToken, TokenXToken, AzureToken)

fun TokenValidationContext.resolveTokens(): List<AccessToken> {
    return validTokens
        .map { it to getJwtToken(it.issuer.name) }
        .mapNotNull { (resolveToken, jwtToken) ->
            jwtToken?.let { AccessToken(it.encodedToken, resolveToken.issuer, it.resolveClaims(resolveToken)) }
        }
}

private fun JwtToken.resolveClaims(resolveToken: ResolveToken): Claims {
    val claims = resolveToken.claims.mapNotNull { claim ->
        val value = jwtTokenClaims.getStringClaim(claim.name)
        value?.let { claim.resolve(value) }?.let { claim to it }
    }.toMap()
    return Claims(claims)
}
