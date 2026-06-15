package no.nav.paw.kafka.signing

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

class KafkaMessageSignerTest : FreeSpec({

    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey
    val publicKey = keyPair.public as ECPublicKey

    val keyBytes = "12345678".toByteArray()
    val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".toByteArray()
    val timestamp = 1_700_000_000_000L
    val value = """{"ident":"12345678901"}""".toByteArray()

    "sign and verify round-trip" {
        val signature = signKafkaRecord(keyBytes, traceparent, timestamp, value, privateKey)
        verifyKafkaRecord(keyBytes, traceparent, timestamp, value, signature, publicKey) shouldBe true
    }

    "verify fails when value is tampered" {
        val signature = signKafkaRecord(keyBytes, traceparent, timestamp, value, privateKey)
        val tampered = value + "x".toByteArray()
        verifyKafkaRecord(keyBytes, traceparent, timestamp, tampered, signature, publicKey) shouldBe false
    }

    "verify fails when key bytes are tampered" {
        val signature = signKafkaRecord(keyBytes, traceparent, timestamp, value, privateKey)
        verifyKafkaRecord("99999999".toByteArray(), traceparent, timestamp, value, signature, publicKey) shouldBe false
    }

    "verify fails when timestamp differs" {
        val signature = signKafkaRecord(keyBytes, traceparent, timestamp, value, privateKey)
        verifyKafkaRecord(keyBytes, traceparent, timestamp + 1, value, signature, publicKey) shouldBe false
    }

    "payload is unambiguous — different splits of same concatenation produce different payloads" {
        val p1 = buildSignaturePayload("AB".toByteArray(), "CD".toByteArray(), timestamp, value)
        val p2 = buildSignaturePayload("A".toByteArray(), "BCD".toByteArray(), timestamp, value)
        (p1.contentEquals(p2)) shouldBe false
    }

    "empty key and traceparent are handled" {
        val signature = signKafkaRecord(ByteArray(0), ByteArray(0), timestamp, value, privateKey)
        verifyKafkaRecord(ByteArray(0), ByteArray(0), timestamp, value, signature, publicKey) shouldBe true
    }
})
