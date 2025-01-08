package no.nav.paw.security.authentication.token

import no.nav.paw.security.authentication.model.Claims
import no.nav.paw.security.authentication.model.Issuer
import no.nav.paw.security.authentication.model.ListClaim
import no.nav.paw.security.authentication.model.Roles
import no.nav.paw.security.authentication.model.Token
import no.nav.paw.security.authentication.model.getValidTokens
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken

data class AccessToken(
    val jwt: String,
    val issuer: Issuer,
    val claims: Claims
) {
    fun isM2MToken(): Boolean {
        return claims.getOrNull(Roles)?.contains("access_as_application") ?: false
    }
}

fun TokenValidationContext.resolveTokens(): List<AccessToken> {
    return getValidTokens()
        .mapNotNull { resolveToken ->
            getJwtToken(resolveToken.issuer.name)?.let { resolveToken to it }
        }
        .map { (resolveToken, jwtToken) ->
            val claims = jwtToken.resolveClaims(resolveToken)
            AccessToken(jwtToken.encodedToken, resolveToken.issuer, claims)
        }
}

private fun JwtToken.resolveClaims(resolveToken: Token): Claims {
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
