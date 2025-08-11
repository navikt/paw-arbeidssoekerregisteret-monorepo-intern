package no.nav.paw.security.texas

data class TexasClientConfig(
    val endpoint: String,
    val target: String,
    val identityProvider: String,
)