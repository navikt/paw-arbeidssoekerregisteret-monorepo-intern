package no.nav.paw.arbeidssokerregisteret.config

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

data class Config(
    val authProviders: AuthProvidersConfig,
    val pdlClientConfig: ServiceClientConfig,
    val poaoTilgangClientConfig: ServiceClientConfig,
    val kafka: KafkaConfig,
    val naisEnv: NaisEnv = currentNaisEnv
)

data class AuthProvidersConfig(
    val azure: AuthProvider,
    val tokenx: AuthProvider
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: List<String>
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class KafkaConfig(
    val brokerUrl: String,
    val schemaRegistryUrl: String,
    val producerId: String,
    val producers: KafkaProducers
) {
    val kafkaProducerProperties
        get() = Properties().apply {
            this[ProducerConfig.CLIENT_ID_CONFIG] = producerId
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java.name
            this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        }
}

data class KafkaProducers(
    val arbeidssokerperiodeStartV1: KafkaTopic
)

data class KafkaTopic(
    val topic: String
)
