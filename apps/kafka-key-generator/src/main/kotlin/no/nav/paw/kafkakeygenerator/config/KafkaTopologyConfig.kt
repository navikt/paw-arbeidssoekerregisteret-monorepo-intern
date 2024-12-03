package no.nav.paw.kafkakeygenerator.config

const val KAFKA_TOPOLOGY_CONFIG = "kafka_topology_config.toml"

data class KafkaTopologyConfig(
    val consumerGroupId: String,
    val hendelseloggTopic: String
)
