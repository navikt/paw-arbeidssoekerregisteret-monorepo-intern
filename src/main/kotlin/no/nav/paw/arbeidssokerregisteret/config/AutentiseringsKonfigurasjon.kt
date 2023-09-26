package no.nav.paw.arbeidssokerregisteret.config

import no.nav.paw.arbeidssokerregisteret.utils.konfigVerdi
import no.nav.security.token.support.v2.RequiredClaims

class AutentiseringsKonfigurasjon(env: Map<String, String>) {
    val authenticationProviders: AuthProviders = listOf(
        AuthProvider(
            name = "tokenx",
            discoveryUrl = env.konfigVerdi("TOKEN_X_WELL_KNOWN_URL"),
            acceptedAudience = listOf(env.konfigVerdi("TOKEN_X_CLIENT_ID")),
            requiredClaims = RequiredClaims("tokenx", arrayOf("acr=Level4", "acr=idporten-loa-high"), true),

        ),
        AuthProvider(
            name = "azure",
            discoveryUrl = env.konfigVerdi("AZURE_APP_WELL_KNOWN_URL"),
            acceptedAudience = listOf(env.konfigVerdi("AZURE_APP_CLIENT_ID")),
            requiredClaims = RequiredClaims("azure", arrayOf("NAVident")),
        ),
    )
}

typealias AuthProviders = List<AuthProvider>

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
    val requiredClaims: RequiredClaims? = null,
)
