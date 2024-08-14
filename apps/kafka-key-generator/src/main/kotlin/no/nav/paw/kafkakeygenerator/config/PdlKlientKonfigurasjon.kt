package no.nav.paw.kafkakeygenerator.config

data class PdlKlientKonfigurasjon(
    val url: String,
    val tema: String,
    val pdlCluster: String,
    val namespace: String,
    val appName: String
)