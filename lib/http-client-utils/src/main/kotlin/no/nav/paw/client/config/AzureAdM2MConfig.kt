package no.nav.paw.client.config

const val AZURE_M2M_CONFIG = "azure_m2m_config.toml"

data class AzureAdM2MConfig(
    val tokenEndpointUrl: String,
    val clientId: String
)