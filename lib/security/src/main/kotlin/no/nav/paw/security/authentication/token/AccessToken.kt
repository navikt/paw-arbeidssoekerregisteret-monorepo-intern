package no.nav.paw.security.authentication.token

import no.nav.paw.security.authentication.model.Claims
import no.nav.paw.security.authentication.model.Issuer
import no.nav.paw.security.authentication.model.ListClaim
import no.nav.paw.security.authentication.model.Roles
import no.nav.paw.security.authentication.model.Token
import no.nav.paw.security.authentication.model.validTokens
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
    return validTokens
        .mapNotNull { token ->
            getJwtToken(token.issuer.name)?.let { token to it }
        }
        .map { (token, jwt) ->
            val claims = jwt.resolveClaims(token)
            AccessToken(jwt.encodedToken, token.issuer, claims)
        }
}

private fun JwtToken.resolveClaims(token: Token): Claims {
    val claims = token.claims
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
