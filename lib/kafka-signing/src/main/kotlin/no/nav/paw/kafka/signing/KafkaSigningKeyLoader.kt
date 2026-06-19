package no.nav.paw.kafka.signing

import no.nav.paw.config.env.Nais
import no.nav.paw.config.env.RuntimeEnvironment
import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * Config keys used as filenames in the mounted NAIS secret.
 */
const val KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY = "PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64"
const val KAFKA_SIGNING_SECRET_KEY_ID = "PAW_SIGNING_KEY_ID"

/**
 * Raw key material loaded from a secret — only lives inside [SigningProducerInterceptor.configure].
 */
internal data class KafkaSigningKeyMaterial(
    val privateKeyPkcs8Base64: String,
    val keyId: String,
)

internal fun loadKafkaSigningKeyMaterial(
    runtimeEnvironment: RuntimeEnvironment,
    mountPath: String,
    localResource: String,
): KafkaSigningKeyMaterial =
    when (runtimeEnvironment) {
        is Nais -> loadKafkaSigningKeyMaterialFromMountedSecret(mountPath)
        else -> loadKafkaSigningKeyMaterialFromLocalResource(localResource)
    }

internal fun loadKafkaSigningKeyMaterialFromMountedSecret(mountPath: String): KafkaSigningKeyMaterial {
    val dir = Path.of(mountPath)
    return KafkaSigningKeyMaterial(
        privateKeyPkcs8Base64 = dir.resolve(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY).toFile().readText().trim(),
        keyId = dir.resolve(KAFKA_SIGNING_SECRET_KEY_ID).toFile().readText().trim(),
    )
}

internal fun loadKafkaSigningKeyMaterialFromLocalResource(resource: String): KafkaSigningKeyMaterial {
    val stream = object {}::class.java.getResourceAsStream(resource)
        ?: File(resource).takeIf { it.isFile }?.inputStream()
        ?: error("Kafka signing key not found at classpath:$resource — add it to src/main/resources/local/")
    val props = stream.use { Properties().apply { load(it) } }
    return KafkaSigningKeyMaterial(
        privateKeyPkcs8Base64 = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY),
        keyId = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_ID),
    )
}

internal fun loadKafkaSigningKeyMaterialFromFile(propertiesFile: File): KafkaSigningKeyMaterial {
    val props = Properties().apply { propertiesFile.inputStream().use { load(it) } }
    return KafkaSigningKeyMaterial(
        privateKeyPkcs8Base64 = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY),
        keyId = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_ID),
    )
}

private fun Properties.requireProperty(key: String): String =
    getProperty(key)?.takeIf { it.isNotBlank() }
        ?: error("Required signing config key '$key' is missing or blank")
