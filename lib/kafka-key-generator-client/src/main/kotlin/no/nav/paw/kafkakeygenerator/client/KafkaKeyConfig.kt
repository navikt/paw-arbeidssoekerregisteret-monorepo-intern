package no.nav.paw.kafkakeygenerator.client

const val KAFKA_KEY_GENERATOR_CLIENT_CONFIG = "kafka_key_generator_client_config.toml"

data class KafkaKeyConfig(
    val url: String,
    val scope: String
)