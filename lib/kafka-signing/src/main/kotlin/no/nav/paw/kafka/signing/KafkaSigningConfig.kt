package no.nav.paw.kafka.signing

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
