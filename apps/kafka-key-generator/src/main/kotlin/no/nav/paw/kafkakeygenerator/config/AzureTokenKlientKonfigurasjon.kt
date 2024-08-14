package no.nav.paw.kafkakeygenerator.config

data class AzureTokenKlientKonfigurasjon(
    val clientId: String,
    val privateJwk: String,
    val tokenEndpointUrl: String
)