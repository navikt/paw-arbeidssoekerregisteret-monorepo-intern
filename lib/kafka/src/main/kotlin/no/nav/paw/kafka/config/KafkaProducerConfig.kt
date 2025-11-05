package no.nav.paw.kafka.config

data class KafkaProducerConfig(
    val version: Int,
    val topic: String,
    val clientIdPrefix: String
) {
    val clientId: String get() = "$clientIdPrefix-v$version-producer"
}
