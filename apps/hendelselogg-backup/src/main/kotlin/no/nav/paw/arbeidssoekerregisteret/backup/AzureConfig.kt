package no.nav.paw.arbeidssoekerregisteret.backup

data class AzureConfig(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claim: String
)