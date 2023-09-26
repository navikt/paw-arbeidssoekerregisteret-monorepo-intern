package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.arbeidssokerregisteret.config.AuthProviders
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.configureAuthentication(authProviders: AuthProviders) {
    authentication {
        authProviders.forEach { authProvider ->
            tokenValidationSupport(
                name = authProvider.name,
                requiredClaims = authProvider.requiredClaims?.let { authProvider.requiredClaims },
                config = TokenSupportConfig(
                    IssuerConfig(
                        name = authProvider.name,
                        discoveryUrl = authProvider.discoveryUrl,
                        acceptedAudience = authProvider.acceptedAudience,
                    ),
                ),
            )
        }
    }
}
