package no.nav.paw.arbeidssokerregisteret.app.config

data class StreamKonfigurasjon(
    val tilstandsDatabase: String,
    val applikasjonsId: String,
    val eventlogTopic: String,
    val periodeTopic: String,
    val situasjonTopic: String
)