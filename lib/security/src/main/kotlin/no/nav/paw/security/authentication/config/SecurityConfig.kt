package no.nav.paw.security.authentication.config

import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.TokenSupportConfig

const val SECURITY_CONFIG = "security_config.toml"

data class SecurityConfig(val authProviders: List<AuthProvider>)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val audiences: List<String> = emptyList(),
    val requiredClaims: AuthProviderRequiredClaims,
    val optionalClaims: AuthProviderOptionalClaims = AuthProviderOptionalClaims()
)

data class AuthProviderRequiredClaims(
    val claims: List<String>,
    val combineWithOr: Boolean = false
)

data class AuthProviderOptionalClaims(
    val claims: List<String> = emptyList(),
)

fun AuthProvider.asTokenSupportConfig(): TokenSupportConfig =
    TokenSupportConfig(this.asIssuerConfig())

fun AuthProvider.asIssuerConfig(): IssuerConfig =
    IssuerConfig(
        name = this.name,
        discoveryUrl = this.discoveryUrl,
        acceptedAudience = this.audiences,
        optionalClaims = this.optionalClaims.claims
    )

fun AuthProvider.asRequiredClaims(): RequiredClaims =
    RequiredClaims(
        this.name,
        this.requiredClaims.claims.toTypedArray(),
        this.requiredClaims.combineWithOr
    )
