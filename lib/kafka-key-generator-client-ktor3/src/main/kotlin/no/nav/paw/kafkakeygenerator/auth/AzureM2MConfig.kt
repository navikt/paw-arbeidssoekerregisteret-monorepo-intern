package no.nav.paw.kafkakeygenerator.auth

const val AZURE_M2M_CONFIG = "azure_m2m.toml"

data class AzureM2MConfig(
    val tokenEndpointUrl: String,
    val clientId: String
)