package no.nav.paw.bekreftelse.api.authz

import no.nav.paw.bekreftelse.api.exception.UfullstendigBearerTokenException
import no.nav.paw.bekreftelse.api.model.Identitetsnummer
import no.nav.security.token.support.core.context.TokenValidationContext
import java.util.*

fun TokenValidationContext.resolveTokens(): AccessToken? {
    return resolveTokens
        .firstOrNull { issuers.contains(it.issuer.name) }
        ?.let { resolveToken ->
            val claims = resolveClaims(resolveToken)
            AccessToken(resolveToken.issuer, claims)
        }
}

private fun TokenValidationContext.resolveClaims(resolveToken: ResolveToken): Claims {
    return resolveToken.claims.mapNotNull { claim ->
        val value = getClaims(resolveToken.issuer.name).getStringClaim(claim.name)
        value?.let { claim.resolve(value) }?.let { claim to it }
    }.toMap()
}

@Suppress("UNCHECKED_CAST")
data class AccessToken(
    val issuer: Issuer,
    val claims: Claims
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(claim: Claim<T>): T =
        claims[claim] as T?
            ?: throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd claim ${claim.name}")

    fun isValidIssuer() = validIssuers.contains(issuer)
}

typealias Claims = Map<Claim<*>, Any>

private sealed class ResolveToken(
    val issuer: Issuer,
    val claims: ResolveClaims
)

private typealias ResolveClaims = List<Claim<*>>

sealed class Issuer(val name: String)

data object IdPorten : Issuer("idporten")
data object TokenX : Issuer("tokenx")
data object Azure : Issuer("azure")

private val validIssuers: List<Issuer> = listOf(IdPorten, TokenX, Azure)

sealed class Claim<A : Any>(
    val name: String,
    val resolve: (String) -> A
)

data object PID : Claim<Identitetsnummer>("pid", ::Identitetsnummer)
data object OID : Claim<UUID>("oid", UUID::fromString)
data object Name : Claim<String>("name", { it })
data object NavIdent : Claim<String>("NAVident", { it })

private data object IdPortenToken : ResolveToken(IdPorten, listOf(PID))
private data object TokenXToken : ResolveToken(TokenX, listOf(PID))
private data object AzureToken : ResolveToken(Azure, listOf(OID, Name, NavIdent))

private val resolveTokens: List<ResolveToken> = listOf(IdPortenToken, TokenXToken, AzureToken)
