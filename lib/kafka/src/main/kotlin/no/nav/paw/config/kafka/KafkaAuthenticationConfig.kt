package no.nav.paw.config.kafka

data class KafkaAuthenticationConfig(
    val truststorePath: String,
    val keystorePath: String,
    val credstorePassword: String
)
