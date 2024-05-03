package no.nav.paw.config.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.util.*
import kotlin.reflect.KClass

class KafkaFactory(private val config: KafkaConfig) {
    val baseProperties =
        Properties().apply {
            this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = config.brokers
            config.authentication?.let { putAll(authenticationConfig(it)) }
            config.schemaRegistry?.let { putAll(schemaRegistryConfig(it)) }
        }

    fun <K : Any, V : Any> createProducer(
        clientId: String,
        keySerializer: KClass<out Serializer<K>>,
        valueSerializer: KClass<out Serializer<V>>,
        acks: String = "all"
    ): Producer<K, V> =
        KafkaProducer(
            baseProperties +
                mapOf(
                    ProducerConfig.ACKS_CONFIG to acks,
                    ProducerConfig.CLIENT_ID_CONFIG to clientId,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer.java
                )
        )

    fun <K : Any, V : Any> createConsumer(
        groupId: String,
        clientId: String,
        keyDeserializer: KClass<out Deserializer<K>>,
        valueDeserializer: KClass<out Deserializer<V>>,
        autoCommit: Boolean = false,
        autoOffsetReset: String = "earliest"
    ): KafkaConsumer<K, V> =
        KafkaConsumer(
            baseProperties +
                mapOf(
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to autoCommit,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
                    ConsumerConfig.GROUP_ID_CONFIG to groupId,
                    ConsumerConfig.CLIENT_ID_CONFIG to clientId,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserializer.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializer.java
                )
        )

    private fun authenticationConfig(config: KafkaAuthenticationConfig): Map<String, Any> =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to config.truststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to config.credstorePassword,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to config.keystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to config.credstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to config.credstorePassword,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to ""
        )

    private fun schemaRegistryConfig(config: KafkaSchemaRegistryConfig): Map<String, Any> =
        mapOf(
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to config.url,
            SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
            SchemaRegistryClientConfig.USER_INFO_CONFIG to "${config.username}:${config.password}",
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to config.autoRegisterSchema,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to config.avroSpecificReaderConfig
        ).apply {
            config.username?.let {
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO"
                SchemaRegistryClientConfig.USER_INFO_CONFIG to "${config.username}:${config.password}"
            }
        }
}

operator fun Properties.plus(other: Map<String, Any>): Properties =
    Properties().apply {
        putAll(this@plus)
        putAll(other)
    }
