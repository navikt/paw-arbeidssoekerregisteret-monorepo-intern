package no.nav.paw.arbeidssoekerregisteret.app

data class ApplicationConfiguration(
    val hendelseloggTopic: String,
    val applicationStreamVersion: String
)