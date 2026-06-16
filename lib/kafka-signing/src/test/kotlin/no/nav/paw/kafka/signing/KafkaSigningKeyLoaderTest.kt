package no.nav.paw.kafka.signing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.util.Base64

class KafkaSigningKeyLoaderTest : FreeSpec({

    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey
    val pkcs8B64 = Base64.getEncoder().encodeToString(privateKey.encoded)
    val keyId = "paw-test-app-ecdsa-v1"

    "loadKafkaSigningKeyMaterialFromMountedSecret reads key and keyId from directory" {
        val dir = Files.createTempDirectory("kafka-signing-test").toFile()
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY).writeText("  $pkcs8B64  \n")
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_ID).writeText("  $keyId  \n")

        val material = loadKafkaSigningKeyMaterialFromMountedSecret(dir.absolutePath)

        material.privateKeyPkcs8Base64 shouldBe pkcs8B64
        material.keyId shouldBe keyId

        dir.deleteRecursively()
    }

    "loadKafkaSigningKeyMaterialFromMountedSecret fails with clear message when file is missing" {
        val dir = Files.createTempDirectory("kafka-signing-test-missing").toFile()

        val ex = shouldThrow<Exception> {
            loadKafkaSigningKeyMaterialFromMountedSecret(dir.absolutePath)
        }
        ex.message shouldContain KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY

        dir.deleteRecursively()
    }

    "loadKafkaSigningKeyMaterialFromFile reads from properties file" {
        val file = Files.createTempFile("kafka-signing", ".properties").toFile()
        file.writeText("""
            $KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY=$pkcs8B64
            $KAFKA_SIGNING_SECRET_KEY_ID=$keyId
        """.trimIndent())

        val material = loadKafkaSigningKeyMaterialFromFile(file)

        material.privateKeyPkcs8Base64 shouldBe pkcs8B64
        material.keyId shouldBe keyId

        file.delete()
    }

    "loadKafkaSigningKeyMaterialFromFile fails with clear message when key is missing" {
        val file = Files.createTempFile("kafka-signing-incomplete", ".properties").toFile()
        file.writeText("$KAFKA_SIGNING_SECRET_KEY_ID=$keyId")

        val ex = shouldThrow<Exception> {
            loadKafkaSigningKeyMaterialFromFile(file)
        }
        ex.message shouldContain KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY

        file.delete()
    }

    "toProducerProperties returns mount path and local resource — not key material" {
        val config = KafkaSigningConfig(
            mountPath = "/var/run/secrets/kafka-signing",
            localResource = "/local/kafka-signing-key.properties",
        )
        val props = config.toProducerProperties()

        props[SigningProducerInterceptor.PAW_SIGNING_MOUNT_PATH] shouldBe "/var/run/secrets/kafka-signing"
        props[SigningProducerInterceptor.PAW_SIGNING_LOCAL_RESOURCE] shouldBe "/local/kafka-signing-key.properties"
        props[org.apache.kafka.clients.producer.ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] shouldBe
            SigningProducerInterceptor::class.java.name
    }
})
