package no.nav.paw.bekreftelse.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val bekreftelseKlienter: List<BekreftelseKlient>
)

data class KafkaTopologyConfig(
    val bekreftelseTargetTopic: String,
    val bekreftelsePaaVegneAvTargetTopic: String
)

data class BekreftelseKlient (
    val applicationIdSuffix: String,
    val paaVegneAvSourceTopic: String,
    val bekreftelseSourceTopic: String
)

@JvmInline
value class ApplicationIdSuffix(val value: String)
