package no.nav.paw.kafka.signing

import no.nav.paw.kafka.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig

/**
 * Signing key configuration loaded at startup.
 *
 * [privateKeyPkcs8Base64] — Base64-encoded PKCS#8 DER bytes of the EC private key.
 * [keyId]                 — Identifies the public key for offline verification,
 *                           e.g. "paw-bekreftelse-tjeneste-ecdsa-v1".
 */
data class KafkaSigningConfig(
    val privateKeyPkcs8Base64: String,
    val keyId: String,
) {
    override fun toString(): String =
        "KafkaSigningConfig(keyId=$keyId, privateKeyPkcs8Base64=***redacted***)"
}


fun KafkaSigningConfig.toProducerProperties(): Map<String, Any> = mapOf(
    ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to SigningProducerInterceptor::class.java.name,
    SigningProducerInterceptor.PAW_SIGNING_KEY_ID to keyId,
    SigningProducerInterceptor.PAW_SIGNING_PRIVATE_KEY_PKCS8 to privateKeyPkcs8Base64,
)

fun KafkaConfig.withRecordSigning(signing: KafkaSigningConfig): KafkaConfig =
    copy(extraProperties = extraProperties + signing.toProducerProperties())
