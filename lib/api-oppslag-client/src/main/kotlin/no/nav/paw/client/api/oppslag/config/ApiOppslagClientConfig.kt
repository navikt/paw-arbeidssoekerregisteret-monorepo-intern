package no.nav.paw.client.api.oppslag.config

const val API_OPPSLAG_CLIENT_CONFIG = "api_oppslag_client_config.toml"

data class ApiOppslagClientConfig(
    val url: String,
    val scope: String
)
