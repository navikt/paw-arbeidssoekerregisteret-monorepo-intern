package no.nav.paw.tilgangskontroll.ktorserver

import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication(authProviders: AuthProviders) {
    val ktorConfig = environment.config
    install(Authentication) {
        authProviders.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                config = ktorConfig,
                requiredClaims = RequiredClaims(
                    issuer = provider.name,
                    claimMap = provider.claims.map.toTypedArray(),
                    combineWithOr = provider.claims.combineWithOr
                )
            )
        }
    }
}