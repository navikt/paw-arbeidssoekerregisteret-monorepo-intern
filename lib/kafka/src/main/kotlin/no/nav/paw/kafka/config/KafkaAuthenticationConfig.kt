package no.nav.paw.kafka.config

data class KafkaAuthenticationConfig(
    val truststorePath: String,
    val keystorePath: String,
    val credstorePassword: String
)
