package no.nav.paw.dolly.api.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val oppslagClientConfig: OppslagClientConfig
)

data class KafkaTopologyConfig(
    val version: Int,
    val antallPartitioner: Int,
    val producerId: String,
    val hendelsesloggTopic: String,
)

data class OppslagClientConfig(
    val url: String,
    val scope: String
)
