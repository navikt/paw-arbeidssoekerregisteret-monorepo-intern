package no.nav.paw.kafkakeygenerator.config

const val AUTHENTICATION_CONFIG = "authentication_config.toml"

data class AuthenticationConfig(
    val providers: List<AuthenticationProviderConfig>,
    val kafkaKeyApiAuthProvider: String
)

data class AuthenticationProviderConfig(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
    val cookieName: String? = null,
    val requiredClaims: List<String>
)
