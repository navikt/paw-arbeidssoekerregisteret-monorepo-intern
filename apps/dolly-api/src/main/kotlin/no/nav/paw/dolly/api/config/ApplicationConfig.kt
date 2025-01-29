package no.nav.paw.dolly.api.config

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig
)

data class KafkaTopologyConfig(
    val version: Int,
    val antallPartitioner: Int,
    val producerId: String,
    val hendelsesloggTopic: String,
)
