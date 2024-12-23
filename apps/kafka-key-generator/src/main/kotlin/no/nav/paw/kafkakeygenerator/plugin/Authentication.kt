package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication(authenticationConfig: AuthenticationConfig) {
    authentication {
        authenticationConfig.providers.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                requiredClaims = RequiredClaims(
                    issuer = provider.name,
                    claimMap = provider.requiredClaims.toTypedArray()
                ),
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = provider.name,
                        discoveryUrl = provider.discoveryUrl,
                        acceptedAudience = provider.acceptedAudience
                    ),
                ),
            )
        }
    }
}
