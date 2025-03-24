package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.config

const val APPLICATION_CONFIG = "application_config.toml"
data class ApplicationConfig(
    val hendelseTopic: String,
    val bekreftelseTopic: String,
    val paaVegneAvTopic: String,
)