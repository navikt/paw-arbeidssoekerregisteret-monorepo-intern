package no.nav.paw.bekreftelse.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig
)

data class KafkaTopologyConfig(
    val bekreftelseTargetTopic: String,
    val bekreftelsePaaVegneAvTargetTopic: String,
    val bekreftelseTeamDagpengerSourceTopic: String,
    val bekreftelsePaaVegneAvTeamDagpengerSourceTopic: String
)
