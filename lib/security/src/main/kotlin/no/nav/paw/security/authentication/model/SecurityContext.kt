package no.nav.paw.security.authentication.model

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.authentication.token.resolveTokens
import no.nav.paw.security.authorization.exception.SecurityContextManglerException
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import no.nav.paw.security.authorization.exception.UgyldigBrukerException
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

data class SecurityContext(
    val bruker: Bruker<*>,
    val accessToken: AccessToken
)

fun AccessToken.sikkerhetsnivaa(): String = "${issuer.name}:${claims.getOrNull(ACR) ?: "undefined"}"

fun ApplicationCall.resolveSecurityContext(): SecurityContext {
    val principal = principal<TokenValidationContextPrincipal>()
    val tokenContext = principal?.context
        ?: throw UgyldigBearerTokenException("Ugyldig eller manglende Bearer Token")

    val accessToken = tokenContext.resolveTokens().firstOrNull() // Kan støtte flere tokens
        ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

    val bruker = when (accessToken.issuer) {
        is TokenX -> {
            logger.debug("TokenX token -> Sluttbruker")
            Sluttbruker(
                ident = accessToken.claims.getOrThrow(PID),
                sikkerhetsnivaa = accessToken.sikkerhetsnivaa(),
            )
        }

        is AzureAd -> {
            if (accessToken.isM2MToken()) {
                val navIdentHeader = request.headers[NavIdentHeader.name]
                if (navIdentHeader.isNullOrBlank()) {
                    logger.debug("AzureAd M2M token -> Anonym")
                    Anonym(accessToken.claims.getOrThrow(OID))
                } else {
                    logger.debug("AzureAd M2M token -> NavAnsatt")
                    NavAnsatt(accessToken.claims.getOrThrow(OID), ident = navIdentHeader, sikkerhetsnivaa = accessToken.sikkerhetsnivaa())
                }
            } else {
                logger.debug("AzureAd token -> NavAnsatt")
                NavAnsatt(accessToken.claims.getOrThrow(OID), accessToken.claims.getOrThrow(NavIdent), accessToken.sikkerhetsnivaa())
            }
        }

        is IdPorten -> {
            logger.debug("IdPorten token -> Sluttbruker")
            Sluttbruker(
                ident = accessToken.claims.getOrThrow(PID),
                sikkerhetsnivaa = accessToken.sikkerhetsnivaa()
            )
        }

        is MaskinPorten -> {
            logger.debug("MaskinPorten token -> Anonym")
            Anonym()
        }
    }

    return SecurityContext(
        bruker = bruker,
        accessToken = accessToken
    )
}

fun ApplicationCall.securityContext(): SecurityContext {
    return authentication.principal<SecurityContext>()
        ?: throw SecurityContextManglerException("Finner ikke security context principal")
}

fun ApplicationCall.securityContext(securityContext: SecurityContext) {
    authentication.principal(securityContext)
}

inline fun <reified T : Bruker<*>> SecurityContext.resolveBruker(): T {
    when (bruker) {
        is T -> return bruker
        else -> throw UgyldigBrukerException("Bruker er ikke av forventet type")
    }
}

inline fun <reified T : Bruker<*>> ApplicationCall.bruker(): T {
    return securityContext().resolveBruker()
}
