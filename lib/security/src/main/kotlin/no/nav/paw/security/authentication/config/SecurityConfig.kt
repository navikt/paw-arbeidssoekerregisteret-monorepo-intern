package no.nav.paw.security.authentication.config

const val SECURITY_CONFIG = "security_config.toml"

data class SecurityConfig(val authProviders: List<AuthProvider>)

data class AuthProvider(
    val name: String,
    val clientId: String,
    val discoveryUrl: String,
    val claims: AuthProviderClaims
)

data class AuthProviderClaims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)
