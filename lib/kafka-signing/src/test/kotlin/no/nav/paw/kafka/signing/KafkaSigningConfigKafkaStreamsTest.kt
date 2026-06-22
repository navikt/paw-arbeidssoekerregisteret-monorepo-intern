package no.nav.paw.kafka.signing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.StreamsConfig

class KafkaSigningConfigKafkaStreamsTest : StringSpec({

    val signingConfig = KafkaSigningConfig(
        mountPath = "/var/run/secrets/kafka-signing",
        localResource = "local/kafka-signing-key.properties",
    )

    "toKafkaStreamsProducerProperties returnerer producer-prefiskerte nøkler" {
        val props = signingConfig.toKafkaStreamsProducerProperties()

        props shouldContainKey StreamsConfig.producerPrefix(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG)
        props shouldContainKey StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH)
        props shouldContainKey StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE)

        props[StreamsConfig.producerPrefix(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG)] shouldBe
            SigningProducerInterceptor::class.java.name
        props[StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH)] shouldBe
            signingConfig.mountPath
        props[StreamsConfig.producerPrefix(SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE)] shouldBe
            signingConfig.localResource
    }

    "toKafkaStreamsProducerProperties inneholder ikke uprefiskerte nøkler" {
        val props = signingConfig.toKafkaStreamsProducerProperties()

        props.keys.all { it.startsWith(StreamsConfig.PRODUCER_PREFIX) } shouldBe true
    }

    "kafkaStreamsConsumerValidationProperties returnerer main.consumer-prefiskert interceptor-nøkkel" {
        val props = kafkaStreamsConsumerValidationProperties()

        val expectedKey = StreamsConfig.mainConsumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG)
        props shouldContainKey expectedKey
        props[expectedKey] shouldBe SignatureValidatingConsumerInterceptor::class.java.name
    }

    "producer og consumer properties kan kombineres uten konflikter" {
        val combined = signingConfig.toKafkaStreamsProducerProperties() +
            kafkaStreamsConsumerValidationProperties()

        combined.size shouldBe 4
        combined.keys.none { key ->
            signingConfig.toKafkaStreamsProducerProperties().containsKey(key) &&
                kafkaStreamsConsumerValidationProperties().containsKey(key)
        } shouldBe true
    }
})
