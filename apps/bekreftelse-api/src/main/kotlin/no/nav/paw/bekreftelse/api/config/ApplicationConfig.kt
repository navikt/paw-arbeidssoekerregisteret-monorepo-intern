package no.nav.paw.bekreftelse.api.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val autorisasjon: AutorisasjonConfig,
    val kafkaTopology: KafkaTopologyConfig
)

data class AutorisasjonConfig(
    val corsAllowOrigins: String? = null
)

data class KafkaTopologyConfig(
    val version: Int,
    val antallPartitioner: Int,
    val producerId: String,
    val consumerId: String,
    val consumerGroupId: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelsesloggTopic: String
)
