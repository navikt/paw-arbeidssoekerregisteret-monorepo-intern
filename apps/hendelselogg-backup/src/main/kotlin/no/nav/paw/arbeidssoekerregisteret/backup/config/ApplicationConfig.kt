package no.nav.paw.arbeidssoekerregisteret.backup.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val consumerVersion: Int,
    val partitionCount: Int,
    val consumerId: String,
    val consumerGroupId: String,
    val hendelsesloggTopic: String,
)