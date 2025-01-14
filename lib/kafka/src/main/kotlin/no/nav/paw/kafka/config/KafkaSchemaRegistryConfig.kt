package no.nav.paw.kafka.config

data class KafkaSchemaRegistryConfig(
    val url: String,
    val username: String?,
    val password: String?,
    val autoRegisterSchema: Boolean = true,
    val avroSpecificReaderConfig: Boolean = true
)
