package no.nav.paw.kafka.signing

import no.nav.paw.config.env.Nais
import no.nav.paw.config.env.RuntimeEnvironment
import java.io.File
import java.nio.file.Path
import java.util.Properties

/**
 * Config keys used as filenames in the mounted NAIS secret.
 */
const val KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY = "PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64"
const val KAFKA_SIGNING_SECRET_KEY_ID = "PAW_SIGNING_KEY_ID"

/**
 * Loads [KafkaSigningConfig] based on [RuntimeEnvironment]:
 * - [Nais]: reads files from a mounted secret directory at [mountPath]
 * - Local: reads from a classpath .properties file at [localResource]
 *
 * ## NAIS secret structure
 * The Kubernetes secret must contain two keys:
 *  - [KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY] — Base64-encoded PKCS#8 DER bytes
 *  - [KAFKA_SIGNING_SECRET_KEY_ID]          — key identifier string
 *
 * Mount via `filesFrom` in nais.yaml — each secret key becomes a file.
 *
 * ## Local development
 * Place a file at `src/main/resources/local/kafka-signing-key.properties`:
 * ```
 * PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64=<base64>
 * PAW_SIGNING_KEY_ID=<key-id>
 * ```
 */
fun loadKafkaSigningConfig(
    runtimeEnvironment: RuntimeEnvironment,
    mountPath: String,
    localResource: String,
): KafkaSigningConfig =
    when (runtimeEnvironment) {
        is Nais -> loadKafkaSigningConfigFromMountedSecret(mountPath)
        else    -> loadKafkaSigningConfigFromLocalResource(localResource)
    }

fun loadKafkaSigningConfigFromMountedSecret(mountPath: String): KafkaSigningConfig {
    val dir = Path.of(mountPath)
    return KafkaSigningConfig(
        privateKeyPkcs8Base64 = dir.resolve(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY).toFile().readText().trim(),
        keyId = dir.resolve(KAFKA_SIGNING_SECRET_KEY_ID).toFile().readText().trim(),
    )
}

fun loadKafkaSigningConfigFromLocalResource(resource: String): KafkaSigningConfig {
    val stream = object {}::class.java.getResourceAsStream(resource)
        ?: error("Kafka signing key not found at classpath:$resource — add it to src/main/resources/local/")
    val props = stream.use { Properties().apply { load(it) } }
    return KafkaSigningConfig(
        privateKeyPkcs8Base64 = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY),
        keyId = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_ID),
    )
}

fun loadKafkaSigningConfigFromFile(propertiesFile: File): KafkaSigningConfig {
    val props = Properties().apply { propertiesFile.inputStream().use { load(it) } }
    return KafkaSigningConfig(
        privateKeyPkcs8Base64 = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY),
        keyId = props.requireProperty(KAFKA_SIGNING_SECRET_KEY_ID),
    )
}

private fun Properties.requireProperty(key: String): String =
    getProperty(key)?.takeIf { it.isNotBlank() }
        ?: error("Required signing config key '$key' is missing or blank")
