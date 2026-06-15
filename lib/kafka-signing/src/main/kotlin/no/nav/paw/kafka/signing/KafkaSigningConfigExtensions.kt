package no.nav.paw.kafka.signing

import org.apache.kafka.clients.producer.ProducerConfig

/**
 * Adds signing interceptor properties to a producer config map.
 *
 * Usage in ApplicationContext:
 * ```kotlin
 * val props = kafkaConfig.toProducerProperties() + signingConfig.toProducerProperties()
 * val producer = KafkaProducer<Long, SpecificRecord>(props)
 * ```
 */
fun KafkaSigningConfig.toProducerProperties(): Map<String, Any> = mapOf(
    ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to SigningProducerInterceptor::class.java.name,
    SigningProducerInterceptor.PAW_SIGNING_KEY_ID to keyId,
    SigningProducerInterceptor.PAW_SIGNING_PRIVATE_KEY_PKCS8 to privateKeyPkcs8Base64,
)
