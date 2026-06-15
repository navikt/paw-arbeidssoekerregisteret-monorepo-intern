package no.nav.paw.kafka.signing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.config.env.Local
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.util.Base64

class KafkaSigningKeyLoaderTest : FreeSpec({

    val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val privateKey = keyPair.private as ECPrivateKey
    val pkcs8B64 = Base64.getEncoder().encodeToString(privateKey.encoded)
    val keyId = "paw-test-app-ecdsa-v1"

    "loadKafkaSigningConfigFromMountedSecret reads key and keyId from directory" {
        val dir = Files.createTempDirectory("kafka-signing-test").toFile()
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY).writeText("  $pkcs8B64  \n")
        dir.resolve(KAFKA_SIGNING_SECRET_KEY_ID).writeText("  $keyId  \n")

        val config = loadKafkaSigningConfigFromMountedSecret(dir.absolutePath)

        config.privateKeyPkcs8Base64 shouldBe pkcs8B64
        config.keyId shouldBe keyId

        dir.deleteRecursively()
    }

    "loadKafkaSigningConfigFromMountedSecret fails with clear message when file is missing" {
        val dir = Files.createTempDirectory("kafka-signing-test-missing").toFile()

        val ex = shouldThrow<Exception> {
            loadKafkaSigningConfigFromMountedSecret(dir.absolutePath)
        }
        ex.message shouldContain KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY

        dir.deleteRecursively()
    }

    "loadKafkaSigningConfigFromFile reads from properties file" {
        val file = Files.createTempFile("kafka-signing", ".properties").toFile()
        file.writeText("""
            $KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY=$pkcs8B64
            $KAFKA_SIGNING_SECRET_KEY_ID=$keyId
        """.trimIndent())

        val config = loadKafkaSigningConfigFromFile(file)

        config.privateKeyPkcs8Base64 shouldBe pkcs8B64
        config.keyId shouldBe keyId

        file.delete()
    }

    "loadKafkaSigningConfigFromFile fails with clear message when key is missing" {
        val file = Files.createTempFile("kafka-signing-incomplete", ".properties").toFile()
        file.writeText("$KAFKA_SIGNING_SECRET_KEY_ID=$keyId")

        val ex = shouldThrow<Exception> {
            loadKafkaSigningConfigFromFile(file)
        }
        ex.message shouldContain KAFKA_SIGNING_SECRET_KEY_PRIVATE_KEY

        file.delete()
    }

    "loadKafkaSigningConfig uses local resource when runtimeEnvironment is Local" {
        val ex = shouldThrow<Exception> {
            loadKafkaSigningConfig(
                runtimeEnvironment = Local,
                mountPath = "/var/run/secrets/kafka-signing",
                localResource = "/local/does-not-exist.properties",
            )
        }
        ex.message shouldContain "does-not-exist.properties"
    }

    "toProducerProperties returns correct keys" {
        val config = KafkaSigningConfig(pkcs8B64, keyId)
        val props = config.toProducerProperties()

        props[SigningProducerInterceptor.PAW_SIGNING_PRIVATE_KEY_PKCS8] shouldBe pkcs8B64
        props[SigningProducerInterceptor.PAW_SIGNING_KEY_ID] shouldBe keyId
        props[org.apache.kafka.clients.producer.ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] shouldBe
            SigningProducerInterceptor::class.java.name
    }
})
