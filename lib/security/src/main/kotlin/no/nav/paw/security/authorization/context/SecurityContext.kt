package no.nav.paw.security.authorization.context

import no.nav.paw.security.authentication.exception.BearerTokenManglerException
import no.nav.paw.security.authentication.exception.UgyldigBearerTokenException
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.M2MToken
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.authentication.token.AzureAd
import no.nav.paw.security.authentication.token.IdPorten
import no.nav.paw.security.authentication.token.NavIdent
import no.nav.paw.security.authentication.token.OID
import no.nav.paw.security.authentication.token.PID
import no.nav.paw.security.authentication.token.TokenX
import no.nav.paw.security.authentication.token.resolveTokens

data class SecurityContext(
    val bruker: Bruker<*>,
    val accessToken: AccessToken
)

fun RequestContext.resolveSecurityContext(): SecurityContext {
    val tokenContext = principal?.context ?: throw BearerTokenManglerException("Bearer Token mangler")

    val accessToken = tokenContext.resolveTokens().firstOrNull() // Kan støtte flere tokens
        ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

    if (accessToken.claims.isEmpty()) {
        throw UgyldigBearerTokenException("Bearer Token mangler påkrevd innhold")
    }

    val bruker = when (accessToken.issuer) {
        is IdPorten -> Sluttbruker(accessToken.claims.getOrThrow(PID))
        is TokenX -> Sluttbruker(accessToken.claims.getOrThrow(PID))
        is AzureAd -> {
            if (accessToken.isM2MToken()) {
                if (headers.navIdent.isNullOrBlank()) {
                    M2MToken(accessToken.claims.getOrThrow(OID))
                } else {
                    NavAnsatt(accessToken.claims.getOrThrow(OID), headers.navIdent)
                }
            } else {
                NavAnsatt(accessToken.claims.getOrThrow(OID), accessToken.claims.getOrThrow(NavIdent))
            }
        }
    }

    return SecurityContext(
        bruker = bruker,
        accessToken = accessToken
    )
}
