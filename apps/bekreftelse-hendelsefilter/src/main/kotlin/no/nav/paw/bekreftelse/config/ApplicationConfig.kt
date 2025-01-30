package no.nav.paw.bekreftelse.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig
)

data class KafkaTopologyConfig(
    val applicationIdSuffix: String,
    val bekreftelseTargetTopic: String,
    val bekreftelsePaaVegneAvTargetTopic: String,
    val teamDagpengerBekreftelseSourceTopic: String,
    val teamDagpengerBekreftelsePaaVegneAvSourceTopic: String
)
