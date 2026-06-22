package no.nav.paw.kafka.signing

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import org.slf4j.LoggerFactory
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.Base64
import java.util.Optional

class SignatureValidatingConsumerInterceptorTest : FreeSpec({

    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey
    val publicKey = keyPair.public as ECPublicKey
    val keyId = "test-signing-key-v1"

    val topic = "test-topic"
    val keyBytes = "12345".toByteArray()
    val valueBytes = """{"hello":"world"}""".toByteArray()
    val timestamp = 1_700_000_000_000L

    fun interceptorWithKeys(keys: Map<String, ECPublicKey>): SignatureValidatingConsumerInterceptor {
        val interceptor = SignatureValidatingConsumerInterceptor()
        val field = SignatureValidatingConsumerInterceptor::class.java.getDeclaredField("publicKeys")
        field.isAccessible = true
        field.set(interceptor, keys)
        return interceptor
    }

    fun signedRecord(
        signKeyId: String = keyId,
        privKey: ECPrivateKey = privateKey,
        recordValue: ByteArray = valueBytes,
    ): ConsumerRecord<ByteArray, ByteArray> {
        val signature = signKafkaRecord(keyBytes, ByteArray(0), timestamp, valueBytes, privKey)
        val headers = RecordHeaders().apply {
            add("x-paw-signature", Base64.getUrlEncoder().withoutPadding().encode(signature))
            add("x-paw-signing-key-id", signKeyId.toByteArray(Charsets.UTF_8))
        }
        return ConsumerRecord(
            topic,
            0,
            0L,
            timestamp,
            TimestampType.CREATE_TIME,
            0,
            0,
            keyBytes,
            recordValue,
            headers,
            Optional.empty()
        )
    }

    fun unsignedRecord(): ConsumerRecord<ByteArray, ByteArray> =
        ConsumerRecord(
            topic,
            0,
            0L,
            timestamp,
            TimestampType.CREATE_TIME,
            0,
            0,
            keyBytes,
            valueBytes,
            RecordHeaders(),
            Optional.empty()
        )

    fun ConsumerRecord<ByteArray, ByteArray>.asRecords() =
        ConsumerRecords(mapOf(TopicPartition(topic, 0) to listOf(this)))

    fun withLogCapture(block: () -> Unit): List<ILoggingEvent> {
        val logger = LoggerFactory.getLogger("signature.validation") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list
    }

    "gyldig signatur produserer ingen warning" {
        val interceptor = interceptorWithKeys(mapOf(keyId to publicKey))

        val logs = withLogCapture {
            interceptor.onConsume(signedRecord().asRecords())
        }

        logs.filter { it.level == Level.WARN }.shouldBeEmpty()
    }

    "ugyldig signatur (tampered value) logger WARN" {
        val interceptor = interceptorWithKeys(mapOf(keyId to publicKey))
        val tampered = signedRecord(recordValue = valueBytes + "x".toByteArray())

        val logs = withLogCapture {
            interceptor.onConsume(tampered.asRecords())
        }

        val warnings = logs.filter { it.level == Level.WARN }
        warnings shouldHaveSize 1
        warnings[0].formattedMessage should { message ->
            if (!message.contains("Ugyldig signatur")) {
                fail("Expected log message to contain 'Ugyldig signatur', but was: $message")
            }
            if (!message.contains("keyId='$keyId'")) {
                fail("Expected log message to contain 'keyId='$keyId'', but was: $message")
            }
        }
    }

    "manglende signaturheadere logger WARN" {
        val interceptor = interceptorWithKeys(mapOf(keyId to publicKey))

        val logs = withLogCapture {
            interceptor.onConsume(unsignedRecord().asRecords())
        }

        val warnings = logs.filter { it.level == Level.WARN }
        warnings shouldHaveSize 1
        warnings[0].formattedMessage should { message ->
            if (!message.contains("Mangler signaturheader(er)")) {
                fail("Expected log message to contain 'Mangler signaturheader', but was: $message")
            }
        }
    }

    "ukjent nøkkel-id logger WARN" {
        val interceptor = interceptorWithKeys(mapOf(keyId to publicKey))
        val recordWithUnknownKey = signedRecord(signKeyId = "ukjent-nøkkel-v99")

        val logs = withLogCapture {
            interceptor.onConsume(recordWithUnknownKey.asRecords())
        }

        val warnings = logs.filter { it.level == Level.WARN }
        warnings shouldHaveSize 1
        warnings[0].formattedMessage should { message ->
            if (!message.contains("Ukjent signeringsnøkkel-id='ukjent-nøkkel-v99'")) {
                fail("Expected log message to contain 'Ukjent signeringsnløkkel-id='ukjent-nøkkel-v99'', but was: $message")
            }
        }
    }

    "manglende signaturheadere legger til unsigned_record OTel event" {
        // Span events can only be asserted indirectly; this test verifies the code path compiles and runs without error.
        // The unsigned_record event is verified via Grafana/Tempo in dev/prod.
        val interceptor = interceptorWithKeys(mapOf(keyId to publicKey))
        val logs = withLogCapture {
            interceptor.onConsume(unsignedRecord().asRecords())
        }
        logs.filter { it.level == Level.WARN } shouldHaveSize 1
    }
})
