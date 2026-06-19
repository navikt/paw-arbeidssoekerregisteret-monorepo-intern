package no.nav.paw.kafka.signing

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldBe
import org.apache.kafka.common.header.internals.RecordHeaders

class StripSigningHeadersTest : FreeSpec({

    "stripSigningHeaders fjerner begge signing-headers" {
        val headers = RecordHeaders().apply {
            add(SIGNATURE_HEADER, "sig".toByteArray())
            add(SIGNING_KEY_ID_HEADER, "key-id-v1".toByteArray())
        }
        val result = stripSigningHeaders(headers)
        result.toArray().shouldBeEmpty()
    }

    "stripSigningHeaders beholder andre headers" {
        val headers = RecordHeaders().apply {
            add("traceparent", "trace-value".toByteArray())
            add(SIGNATURE_HEADER, "sig".toByteArray())
            add(SIGNING_KEY_ID_HEADER, "key-id-v1".toByteArray())
            add("x-custom", "custom-value".toByteArray())
        }
        val result = stripSigningHeaders(headers)
        result.toArray().map { it.key() } shouldBe listOf("traceparent", "x-custom")
    }

    "stripSigningHeaders muterer ikke original headers" {
        val headers = RecordHeaders().apply {
            add(SIGNATURE_HEADER, "sig".toByteArray())
            add(SIGNING_KEY_ID_HEADER, "key-id-v1".toByteArray())
            add("traceparent", "trace-value".toByteArray())
        }
        stripSigningHeaders(headers)
        headers.toArray().size shouldBe 3
    }

    "stripSigningHeaders returnerer ny instans" {
        val headers = RecordHeaders().apply {
            add("traceparent", "trace-value".toByteArray())
        }
        val result = stripSigningHeaders(headers)
        (result === headers) shouldBe false
    }

    "stripSigningHeaders på headers uten signing-headers beholder alt" {
        val headers = RecordHeaders().apply {
            add("traceparent", "trace-value".toByteArray())
            add("x-custom", "value".toByteArray())
        }
        val result = stripSigningHeaders(headers)
        result.toArray().map { it.key() } shouldBe listOf("traceparent", "x-custom")
    }

    "stripSigningHeaders på tomme headers returnerer tomme headers" {
        val result = stripSigningHeaders(RecordHeaders())
        result.toArray().shouldBeEmpty()
    }
})
