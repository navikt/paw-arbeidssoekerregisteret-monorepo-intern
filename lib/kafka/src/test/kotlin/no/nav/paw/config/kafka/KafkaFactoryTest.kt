package no.nav.paw.config.kafka

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile

class KafkaFactoryTest : StringSpec({
    "setter riktig security protocol" {
        val truststoreFile = createTempFile("truststore", ".jks")
        val keystoreFile = createTempFile("keystore", ".jks")
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                authentication =
                    KafkaAuthenticationConfig(
                        truststorePath = truststoreFile.absolutePathString(),
                        keystorePath = keystoreFile.absolutePathString(),
                        credstorePassword = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["security.protocol"] shouldBe "SSL"
    }
    "setter riktig schema registry url" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                schemaRegistry =
                    KafkaSchemaRegistryConfig(
                        url = "http://localhost:8081",
                        username = "username",
                        password = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["schema.registry.url"] shouldBe "http://localhost:8081"
    }
    "setter riktig schema registry basic auth" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092",
                schemaRegistry =
                    KafkaSchemaRegistryConfig(
                        url = "http://localhost:8081",
                        username = "username",
                        password = "password"
                    )
            )

        val factory = KafkaFactory(config)

        factory.baseProperties["basic.auth.credentials.source"] shouldBe "USER_INFO"
        factory.baseProperties["basic.auth.user.info"] shouldBe "username:password"
    }
    "lager kafka producer" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092"
            )

        val factory = KafkaFactory(config)

        val producer =
            factory.createProducer(
                "testId",
                Serdes.String().serializer()::class,
                Serdes.String().serializer()::class
            )

        producer.javaClass shouldBe KafkaProducer::class.java
    }
    "lager kafka consumer" {
        val config =
            KafkaConfig(
                brokers = "localhost:9092"
            )

        val factory = KafkaFactory(config)

        val consumer =
            factory.createConsumer(
                groupId = "groupId",
                clientId = "clientId",
                keyDeserializer = Serdes.String().deserializer()::class,
                valueDeserializer = Serdes.String().deserializer()::class
            )
        consumer.javaClass shouldBe KafkaConsumer::class.java
    }
})
