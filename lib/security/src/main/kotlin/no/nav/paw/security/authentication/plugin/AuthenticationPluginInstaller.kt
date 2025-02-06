package no.nav.paw.security.authentication.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.asRequiredClaims
import no.nav.paw.security.authentication.config.asTokenSupportConfig
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.installAuthenticationPlugin(providers: List<AuthProvider>) {
    authentication {
        providers.forEach { provider ->
            tokenValidationSupport(
                name = provider.name,
                config = provider.asTokenSupportConfig(),
                requiredClaims = provider.asRequiredClaims(),
            )
        }
    }
}
