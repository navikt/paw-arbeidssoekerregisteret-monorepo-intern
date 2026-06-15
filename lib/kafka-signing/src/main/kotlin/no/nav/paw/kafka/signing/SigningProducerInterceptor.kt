package no.nav.paw.kafka.signing

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

private val BASE64URL = Base64.getUrlEncoder().withoutPadding()
private val BASE64DEC = Base64.getDecoder()
private val logger = LoggerFactory.getLogger(SigningProducerInterceptor::class.java)

/**
 * Kafka [ProducerInterceptor] that adds ECDSA signature headers to every outgoing record.
 *
 * Added headers:
 *  - `x-paw-signature`    : Base64url (no padding) encoded DER signature
 *  - `x-paw-signing-key-id`: UTF-8 string identifying the public key for offline verification
 *
 * Required producer properties:
 *  - [PAW_SIGNING_KEY_ID]            : String — key identifier, e.g. "ecdsa-v1"
 *  - [PAW_SIGNING_PRIVATE_KEY_PKCS8] : Base64-encoded PKCS#8 DER bytes of the EC private key
 *
 * The interceptor re-instantiates the configured key and value serializers so that the
 * signature covers the actual serialized bytes, regardless of whether JSON, Avro, or any
 * other serializer is used.
 */
class SigningProducerInterceptor<K, V> : ProducerInterceptor<K, V> {

    private lateinit var keySerializer: Serializer<K>
    private lateinit var valueSerializer: Serializer<V>
    private lateinit var privateKey: ECPrivateKey
    private lateinit var keyId: ByteArray

    companion object {
        const val PAW_SIGNING_KEY_ID = "paw.signing.key.id"
        const val PAW_SIGNING_PRIVATE_KEY_PKCS8 = "paw.signing.private.key.pkcs8.base64"
    }

    override fun configure(configs: Map<String, *>) {
        keyId = (configs[PAW_SIGNING_KEY_ID] as String).trim().toByteArray(Charsets.UTF_8)

        val pkcs8Bytes = BASE64DEC.decode((configs[PAW_SIGNING_PRIVATE_KEY_PKCS8] as String).trim())
        privateKey = KeyFactory.getInstance("EC")
            .generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes)) as ECPrivateKey

        keySerializer = instantiateSerializer(configs, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, isKey = true)
        valueSerializer = instantiateSerializer(configs, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, isKey = false)
    }

    override fun onSend(record: ProducerRecord<K, V>): ProducerRecord<K, V> {
        val topic = record.topic()
        val headers = record.headers()

        return try {
            val kBytes = record.key()?.let { keySerializer.serialize(topic, headers, it) } ?: ByteArray(0)
            val vBytes = record.value()?.let { valueSerializer.serialize(topic, headers, it) } ?: ByteArray(0)
            val traceparentBytes = headers.lastHeader("traceparent")?.value() ?: ByteArray(0)

            // Fix the timestamp so that what we sign matches what the broker stores.
            val timestamp = record.timestamp() ?: System.currentTimeMillis()

            val signature = signKafkaRecord(kBytes, traceparentBytes, timestamp, vBytes, privateKey)

            // Return a new record with the explicit timestamp and signature headers.
            ProducerRecord(topic, record.partition(), timestamp, record.key(), record.value(), headers).also {
                it.headers().add("x-paw-signature", BASE64URL.encode(signature))
                it.headers().add("x-paw-signing-key-id", keyId)
            }
        } catch (e: Exception) {
            logger.error("Failed to sign Kafka record on topic={}, record will be sent unsigned", topic, e)
            record
        }
    }

    override fun onAcknowledgement(metadata: RecordMetadata?, exception: Exception?) = Unit
    override fun close() {
        keySerializer.close()
        valueSerializer.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> instantiateSerializer(
        configs: Map<String, *>,
        configKey: String,
        isKey: Boolean,
    ): Serializer<T> {
        val serializerClass = when (val value = configs[configKey]) {
            is Class<*> -> value
            is String -> Class.forName(value)
            else -> error("$configKey is missing or has unexpected type: $value")
        }
        return (serializerClass.getDeclaredConstructor().newInstance() as Serializer<T>)
            .also { it.configure(configs, isKey) }
    }
}
