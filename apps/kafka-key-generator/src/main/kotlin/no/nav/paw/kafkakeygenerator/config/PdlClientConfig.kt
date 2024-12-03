package no.nav.paw.kafkakeygenerator.config

const val PDL_CLIENT_CONFIG = "pdl_client_config.toml"

data class PdlClientConfig(
    val url: String,
    val scope: String,
    val tema: String
)