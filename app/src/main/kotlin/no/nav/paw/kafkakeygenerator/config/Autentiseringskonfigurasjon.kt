package no.nav.paw.kafkakeygenerator.config

data class Autentiseringskonfigurasjon(
    val discoveryUrl: String,
    val acceptedAudience: String,
    val name: String
)
