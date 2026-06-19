package no.nav.paw.kafka.signing

import no.nav.paw.kafka.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig

/**
 * Signing configuration passed through Kafka producer properties.
 * Contains only non-sensitive path references — the private key is loaded
 * by [SigningProducerInterceptor] at startup using [no.nav.paw.config.env.currentRuntimeEnvironment].
 *
 * [mountPath]     — directory where the Nais secret is mounted (Nais environments)
 * [localResource] — classpath resource path for local development
 */
data class KafkaSigningConfig(
    val mountPath: String,
    val localResource: String,
)

fun KafkaSigningConfig.toProducerProperties(): Map<String, Any> = mapOf(
    ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to SigningProducerInterceptor::class.java.name,
    SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH to mountPath,
    SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE to localResource,
)

fun KafkaConfig.withRecordSigning(signing: KafkaSigningConfig): KafkaConfig =
    copy(extraProperties = extraProperties + signing.toProducerProperties())
