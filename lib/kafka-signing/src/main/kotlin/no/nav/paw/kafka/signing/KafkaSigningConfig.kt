package no.nav.paw.kafka.signing

import no.nav.paw.kafka.config.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.StreamsConfig

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
    copy(producerExtraProperties = producerExtraProperties + signing.toProducerProperties())

/**
 * Returns a [StreamsConfig]-ready property map that configures [SigningProducerInterceptor]
 * on the Kafka Streams internal producer.
 *
 * All keys are prefixed with [StreamsConfig.PRODUCER_PREFIX] so that Kafka Streams
 * correctly isolates them to the producer and does not apply them to consumers.
 *
 * Usage:
 * ```kotlin
 * val streamsConfig = baseProperties + signingConfig.toKafkaStreamsProducerProperties()
 * ```
 */
fun KafkaSigningConfig.toKafkaStreamsProducerProperties(): Map<String, Any> = mapOf(
    StreamsConfig.producerPrefix(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG) to SigningProducerInterceptor::class.java.name,
    StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH) to mountPath,
    StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE) to localResource,
)

/**
 * Returns a [StreamsConfig]-ready property map that configures [SignatureValidatingConsumerInterceptor]
 * on the Kafka Streams main consumer only (not restore or global consumers).
 *
 * Usage:
 * ```kotlin
 * val streamsConfig = baseProperties + kafkaStreamsConsumerValidationProperties()
 * ```
 */
fun kafkaStreamsConsumerValidationProperties(): Map<String, Any> = mapOf(
    StreamsConfig.mainConsumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG) to
        SignatureValidatingConsumerInterceptor::class.java.name
)
