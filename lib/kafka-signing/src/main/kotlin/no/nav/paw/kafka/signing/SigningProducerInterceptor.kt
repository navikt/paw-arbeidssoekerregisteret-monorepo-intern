package no.nav.paw.kafka.signing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.config.env.currentRuntimeEnvironment
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import no.nav.paw.logging.logger.TeamLogsLogger
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

private val BASE64URL = Base64.getUrlEncoder().withoutPadding()
private val BASE64_VALID_CHARS = Regex("[^A-Za-z0-9+/\\-_]")
private val logger = LoggerFactory.getLogger("signature.signing")

/**
 * Decodes a PKCS#8 private key from Base64. Handles:
 * - Plain Base64 (standard or URL-safe alphabet)
 * - PEM format (strips -----BEGIN/END PRIVATE KEY----- headers)
 * - Missing or incorrect padding
 * - Embedded whitespace, newlines, BOM, and other non-Base64 characters
 */
internal fun decodePkcs8Key(input: String): ByteArray {
    // Keep only valid Base64 characters; discards newlines, spaces, BOM, CRLF, invisible unicode, etc.
    val cleaned = input
        .lines()
        .filterNot { it.trimStart().startsWith("-----") }
        .joinToString("")
        .replace(BASE64_VALID_CHARS, "")
        .trimEnd('=')
    val padded = when (cleaned.length % 4) {
        0 -> cleaned
        2 -> "$cleaned=="
        3 -> "$cleaned="
        else -> error(
            "Invalid Base64 key: cleaned length ${cleaned.length} gives mod4=${cleaned.length % 4}. " +
                    "Expected length divisible by 4 (e.g. 184 for P-256). " +
                    "Re-encode with: openssl pkcs8 -topk8 -nocrypt -in ec-private.pem -outform DER | base64 -w0"
        )
    }
    logger.debug(
        "decodePkcs8Key: input.length={} cleaned.length={} padded.length={} mod4={}",
        input.length, cleaned.length, padded.length, cleaned.length % 4
    )
    // Normalise URL-safe alphabet (- _) to standard (+ /) before decoding
    return Base64.getDecoder().decode(padded.replace('-', '+').replace('_', '/'))
}

/**
 * Kafka [ProducerInterceptor] that adds ECDSA signature headers to every outgoing record.
 *
 * Added headers:
 *  - `x-paw-signature`    : Base64url (no padding) encoded DER signature
 *  - `x-paw-signing-key-id`: UTF-8 string identifying the public key for offline verification
 *
 * Required producer properties:
 *  - [PAW_SIGNING_MOUNT_PATH]     : path to mounted Nais secret directory (Nais environments)
 *  - [PAW_SIGNING_LOCAL_RESOURCE] : classpath resource or absolute file path (local development)
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
    private lateinit var keyIdString: String

    companion object {
        const val PAW_SIGNING_MOUNT_PATH = "paw.signing.mount.path"
        const val PAW_SIGNING_LOCAL_RESOURCE = "paw.signing.local.resource"
    }

    override fun configure(configs: Map<String, *>) {
        val mountPath = configs[PAW_SIGNING_MOUNT_PATH] as String
        val localResource = configs[PAW_SIGNING_LOCAL_RESOURCE] as String
        val keyMaterial = loadKafkaSigningKeyMaterial(currentRuntimeEnvironment, mountPath, localResource)
        keyIdString = keyMaterial.keyId.trim()
        keyId = keyIdString.toByteArray(Charsets.UTF_8)

        val pkcs8Bytes = decodePkcs8Key(keyMaterial.privateKeyPkcs8Base64)
        privateKey = KeyFactory.getInstance("EC")
            .generatePrivate(PKCS8EncodedKeySpec(pkcs8Bytes)) as ECPrivateKey

        keySerializer = instantiateSerializer(configs, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, isKey = true)
        valueSerializer = instantiateSerializer(configs, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, isKey = false)
    }

    @WithSpan("signing_interceptor_on_send")
    override fun onSend(record: ProducerRecord<K, V>): ProducerRecord<K, V> {
        val topic = record.topic()
        val headers = record.headers()

        return try {
            Span.current().setAttribute("key_id", keyIdString)
            val kBytes = record.key()?.let { keySerializer.serialize(topic, headers, it) } ?: ByteArray(0)
            val vBytes = record.value()?.let { valueSerializer.serialize(topic, headers, it) } ?: ByteArray(0)
            val traceparentBytes = headers.lastHeader("traceparent")?.value() ?: ByteArray(0)

            // Fix the timestamp so that what we sign matches what the broker stores.
            val timestamp = record.timestamp() ?: System.currentTimeMillis()

            val signature = signKafkaRecord(kBytes, traceparentBytes, timestamp, vBytes, privateKey)

            // Return a new record with the explicit timestamp and signature headers.
            ProducerRecord(topic, record.partition(), timestamp, record.key(), record.value(), headers).also {
                it.headers().add(SIGNATURE_HEADER, BASE64URL.encode(signature))
                it.headers().add(SIGNING_KEY_ID_HEADER, keyId)
            }
        } catch (e: Exception) {
            TeamLogsLogger.error(
                "Failed to sign Kafka record on topic={}, key={}, record will be sent unsigned",
                topic,
                record.key(),
                e
            )
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
