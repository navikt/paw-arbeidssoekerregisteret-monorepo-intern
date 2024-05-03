package no.nav.paw.config.kafka

const val KAFKA_CONFIG_WITH_SCHEME_REG = "kafka_configuration_schemareg.toml"
const val KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG = "kafka_streams_configuration_schemareg.toml"
const val KAFKA_CONFIG = "kafka_configuration.toml"

data class KafkaConfig(
    val brokers: String,
    val authentication: KafkaAuthenticationConfig? = null,
    val schemaRegistry: KafkaSchemaRegistryConfig? = null,
    val applicationIdPrefix: String? = null
)
