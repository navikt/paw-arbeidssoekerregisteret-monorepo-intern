package no.nav.paw.kafka.signing

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.nio.ByteBuffer
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/**
 * Canonical signed payload format (length-prefixed to avoid ambiguity):
 *
 *   [4B len][key bytes]
 *   [4B len][traceparent bytes (UTF-8, empty if absent)]
 *   [8B    ][timestamp millis, big-endian]
 *   [4B len][value bytes]
 *
 * Algorithm: SHA256withECDSA (DER-encoded output)
 */
@WithSpan("sign_kafka_record")
fun signKafkaRecord(
    keyBytes: ByteArray,
    traceparentBytes: ByteArray,
    timestampMs: Long,
    valueBytes: ByteArray,
    privateKey: ECPrivateKey,
): ByteArray =
    Signature.getInstance("SHA256withECDSA").run {
        initSign(privateKey)
        update(buildSignaturePayload(keyBytes, traceparentBytes, timestampMs, valueBytes))
        sign()
    }

@WithSpan("verify_kafka_record")
fun verifyKafkaRecord(
    keyBytes: ByteArray,
    traceparentBytes: ByteArray,
    timestampMs: Long,
    valueBytes: ByteArray,
    signatureBytes: ByteArray,
    publicKey: ECPublicKey,
): Boolean =
    Signature.getInstance("SHA256withECDSA").run {
        initVerify(publicKey)
        update(buildSignaturePayload(keyBytes, traceparentBytes, timestampMs, valueBytes))
        verify(signatureBytes)
    }

fun buildSignaturePayload(
    keyBytes: ByteArray,
    traceparentBytes: ByteArray,
    timestampMs: Long,
    valueBytes: ByteArray,
): ByteArray =
    ByteBuffer.allocate(4 + keyBytes.size + 4 + traceparentBytes.size + 8 + 4 + valueBytes.size)
        .putInt(keyBytes.size).put(keyBytes)
        .putInt(traceparentBytes.size).put(traceparentBytes)
        .putLong(timestampMs)
        .putInt(valueBytes.size).put(valueBytes)
        .array()
