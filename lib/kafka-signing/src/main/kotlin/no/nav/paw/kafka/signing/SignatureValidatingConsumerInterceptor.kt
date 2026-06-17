package no.nav.paw.kafka.signing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.apache.kafka.clients.consumer.ConsumerInterceptor
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

private val logger = LoggerFactory.getLogger(SignatureValidatingConsumerInterceptor::class.java)

class SignatureValidatingConsumerInterceptor : ConsumerInterceptor<ByteArray, ByteArray> {

    private var publicKeys: Map<String, ECPublicKey> = emptyMap()
    private var publicKeysCount: Int = -1

    override fun configure(configs: Map<String, *>) {
        publicKeys = loadPublicKeysFromClasspath()
        publicKeysCount = publicKeys.size
        logger.info("SignatureValidatingConsumerInterceptor konfigurert følgende public keys:", publicKeys.keys)
    }

    @WithSpan("validate_signatures")
    override fun onConsume(records: ConsumerRecords<ByteArray, ByteArray>): ConsumerRecords<ByteArray, ByteArray> {
        Span.current().setAttribute("public.keys.loaded", publicKeys.size.toString())
        if (publicKeys.isEmpty()) return records
        for (record in records) {
            try {
                valider(
                    topic = record.topic(),
                    keyBytes = record.key() ?: ByteArray(0),
                    valueBytes = record.value() ?: ByteArray(0),
                    timestampMs = record.timestamp(),
                    traceparentBytes = record.headers().lastHeader("traceparent")?.value() ?: ByteArray(0),
                    signatureHeader = record.headers().lastHeader("x-paw-signature")?.value(),
                    keyIdHeader = record.headers().lastHeader("x-paw-signing-key-id")?.value(),
                )
            } catch (e: Exception) {
                logger.error(
                    "Teknisk feil ved signaturvalidering — topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e
                )
            }
        }
        return records
    }

    private fun valider(
        topic: String,
        keyBytes: ByteArray,
        valueBytes: ByteArray,
        timestampMs: Long,
        traceparentBytes: ByteArray,
        signatureHeader: ByteArray?,
        keyIdHeader: ByteArray?,
    ) {
        if (signatureHeader == null || keyIdHeader == null) {
            logger.warn(
                "Mangler signaturheader(er) — topic={}, harSignatur={}, harNøkkelId={}",
                topic, signatureHeader != null, keyIdHeader != null
            )
            Span.current().addEvent("missing_signature_headers")
            return
        }

        val keyId = String(keyIdHeader, Charsets.UTF_8)
        Span.current().setAttribute("record_key_id", keyId)
        val publicKey = publicKeys[keyId]
        if (publicKey == null) {
            logger.warn("Ukjent signeringsnøkkel-id='{}' — topic={}", keyId, topic)
            Span.current().addEvent("unknown_key_id")
            return
        }

        val signatureBytes = Base64.getUrlDecoder().decode(signatureHeader)
        val gyldig = verifyKafkaRecord(
            keyBytes = keyBytes,
            traceparentBytes = traceparentBytes,
            timestampMs = timestampMs,
            valueBytes = valueBytes,
            signatureBytes = signatureBytes,
            publicKey = publicKey
        )

        if (!gyldig) {
            logger.warn("Ugyldig signatur — topic={}, keyId='{}'", topic, keyId)
        }
    }

    override fun onCommit(offsets: Map<TopicPartition, OffsetAndMetadata>) = Unit
    override fun close() = Unit
}

fun loadPublicKeysFromClasspath(): Map<String, ECPublicKey> {
    val indexStream = SignatureValidatingConsumerInterceptor::class.java
        .getResourceAsStream("/paw-signing-public-keys/index")
        ?: run {
            logger.warn("Ingen nøkkelindeks funnet (/paw-signing-public-keys/index) — signaturvalidering deaktivert")
            return emptyMap()
        }

    val keyIds = indexStream.use { it.bufferedReader().readLines() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val result = mutableMapOf<String, ECPublicKey>()
    for (keyId in keyIds) {
        runCatching {
            val resourcePath = "/paw-signing-public-keys/$keyId.pub.b64"
            val rawB64 = SignatureValidatingConsumerInterceptor::class.java
                .getResourceAsStream(resourcePath)
                ?.use { it.readBytes().toString(Charsets.UTF_8).trim() }
                ?: error("Fant ikke $resourcePath på classpath")
            val keyBytes = Base64.getDecoder().decode(rawB64)
            result[keyId] = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(keyBytes)) as ECPublicKey
        }.getOrElse { e ->
            throw Exception("Klarte ikke laste offentlig nøkkel for keyId='$keyId'", e)
        }
    }

    logger.info("Lastet {} signeringsnøkkel(er): {}", result.size, result.keys)
    return result
}
