package no.nav.paw.dolly.api.producer

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.dolly.api.config.ApplicationConfig
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafka.signing.SIGNATURE_HEADER
import no.nav.paw.kafka.signing.SIGNING_KEY_ID_HEADER
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

// Static stale signature copied from a real dev record — will always fail validation.
// Used to verify that consumers handle invalid signatures correctly in dev.
private const val STATIC_INVALID_SIGNATURE =
    "MEUCIA73nZrR2EUuzhyMHfOSQAw7ssINrl0JefWJh9tDxvuOAiEAwia2zAoyV5Oh35TGlg7ouofeK3RXvaidprmeMh7W4zQ"
private const val STATIC_SIGNING_KEY_ID = "paw-api-inngang-kafka-signing-key-v2"

class HendelseKafkaProducer(
    private val applicationConfig: ApplicationConfig,
    private val producer: Producer<Long, Hendelse>
) {
    private val logger = buildLogger

    fun sendHendelse(key: Long, message: Hendelse) = runBlocking {
        val topic = applicationConfig.kafkaTopology.hendelsesloggTopic
        val record = ProducerRecord<Long, Hendelse>(topic, key, message).also {
            it.headers().add(SIGNATURE_HEADER, STATIC_INVALID_SIGNATURE.toByteArray(Charsets.UTF_8))
            it.headers().add(SIGNING_KEY_ID_HEADER, STATIC_SIGNING_KEY_ID.toByteArray(Charsets.UTF_8))
        }

        val metadata = producer.sendDeferred(record).await()
        logger.debug(
            "Sender melding til Kafka topic {} (partition={}, offset={})",
            topic,
            metadata.partition(),
            metadata.offset()
        )
    }
}
