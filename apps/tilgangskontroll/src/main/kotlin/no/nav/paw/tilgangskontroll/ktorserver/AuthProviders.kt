package no.nav.paw.tilgangskontroll.ktorserver

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

fun authProvidersOf(vararg authProviders: AuthProvider): AuthProviders =
    authProviders
        .map { provider -> loadNaisOrLocalConfiguration<AuthProviderConfig>(provider.config) }
        .let(::AuthProviders)

class AuthProviders(authProviders: List<AuthProviderConfig>): List<AuthProviderConfig> by authProviders

sealed class AuthProvider(
    val name: String,
    val config: String
) {
    data object IdPorten : AuthProvider(name = "idporten", config = "auth_provider_id_porten.toml")
    data object EntraId : AuthProvider(name = "azure", config = "auth_provider_entra_id.toml")
    data object TokenX : AuthProvider(name = "tokenx", config = "auth_provider_tokenx.toml")
}

data class AuthProviderConfig(
    val name: String,
    val clientId: String,
    val discoveryUrl: String,
    val claims: AuthProviderClaims
)

data class AuthProviderClaims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)
