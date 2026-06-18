package no.nav.paw.kafka.signing

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
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
private const val INSTRUMENTATION_SCOPE = "kafka-signing"

class SignatureValidatingConsumerInterceptor : ConsumerInterceptor<ByteArray, ByteArray> {

    private var publicKeys: Map<String, ECPublicKey> = emptyMap()

    override fun configure(configs: Map<String, *>) {
        publicKeys = loadPublicKeysFromClasspath()
        logger.info("SignatureValidatingConsumerInterceptor konfigurert følgende public keys:", publicKeys.keys)
    }

    override fun onConsume(records: ConsumerRecords<ByteArray, ByteArray>): ConsumerRecords<ByteArray, ByteArray> {
        if (publicKeys.isEmpty()) return records
        for (record in records) {
            val traceparentBytes = record.headers().lastHeader("traceparent")?.value() ?: ByteArray(0)
            val (span, scope) = spanFromTraceparent(traceparentBytes)
            try {
                valider(
                    topic = record.topic(),
                    keyBytes = record.key() ?: ByteArray(0),
                    valueBytes = record.value() ?: ByteArray(0),
                    timestampMs = record.timestamp(),
                    traceparentBytes = traceparentBytes,
                    signatureHeader = record.headers().lastHeader("x-paw-signature")?.value(),
                    keyIdHeader = record.headers().lastHeader("x-paw-signing-key-id")?.value(),
                )
            } catch (e: Exception) {
                logger.error(
                    "Teknisk feil ved signaturvalidering — topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset(), e
                )
            } finally {
                span.end()
                scope.close()
            }
        }
        return records
    }

    @WithSpan("validate_signature")
    private fun valider(
        topic: String,
        keyBytes: ByteArray,
        valueBytes: ByteArray,
        timestampMs: Long,
        traceparentBytes: ByteArray,
        signatureHeader: ByteArray?,
        keyIdHeader: ByteArray?,
    ) {
        // Attach this span to the producer's trace via the record's traceparent header,
        // so validation appears in the correct message timeline rather than as a root span.
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
            Span.current().addEvent("invalid_signature")
        } else {
            Span.current().addEvent("valid_signature")
        }
    }

    override fun onCommit(offsets: Map<TopicPartition, OffsetAndMetadata>) = Unit
    override fun close() = Unit
}

/**
 * Creates a span parented to the remote trace identified by [traceparentBytes].
 * Returns the new span and its [io.opentelemetry.context.Scope] — both must be closed by the caller.
 *
 * If [traceparentBytes] is missing or cannot be parsed the span is created without a parent,
 * which is still preferable to the old batch-level root span since it covers exactly one record.
 */
private fun spanFromTraceparent(traceparentBytes: ByteArray): Pair<Span, io.opentelemetry.context.Scope> {
    val tracer = GlobalOpenTelemetry.get().tracerProvider.get(INSTRUMENTATION_SCOPE)
    val parentContext = traceparentBytes
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { String(it, Charsets.UTF_8) }.getOrNull() }
        ?.split("-")
        ?.takeIf { it.size == 4 }
        ?.let { parts ->
            runCatching {
                SpanContext.createFromRemoteParent(
                    parts[1],
                    parts[2],
                    TraceFlags.getSampled(),
                    TraceState.getDefault()
                )
            }.getOrNull()
        }
        ?.takeIf { it.isValid }
        ?.let { spanContext -> Context.current().with(Span.wrap(spanContext)) }
        ?: Context.current()

    val span = tracer.spanBuilder("validate_signature")
        .setParent(parentContext)
        .startSpan()
    val scope = parentContext.with(span).makeCurrent()
    return span to scope
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
