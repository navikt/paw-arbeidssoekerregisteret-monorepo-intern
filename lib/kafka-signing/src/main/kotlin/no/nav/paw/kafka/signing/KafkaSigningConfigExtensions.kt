package no.nav.paw.kafka.signing

import no.nav.paw.kafka.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerConfig

fun KafkaSigningConfig.toProducerProperties(): Map<String, Any> = mapOf(
    ProducerConfig.INTERCEPTOR_CLASSES_CONFIG to SigningProducerInterceptor::class.java.name,
    SigningProducerInterceptor.PAW_SIGNING_KEY_ID to keyId,
    SigningProducerInterceptor.PAW_SIGNING_PRIVATE_KEY_PKCS8 to privateKeyPkcs8Base64,
)

fun KafkaConfig.withRecordSigning(signing: KafkaSigningConfig): KafkaConfig =
    copy(extraProperties = extraProperties + signing.toProducerProperties())
