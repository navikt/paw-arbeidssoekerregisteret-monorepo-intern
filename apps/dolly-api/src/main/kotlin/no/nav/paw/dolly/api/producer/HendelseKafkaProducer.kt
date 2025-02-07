package no.nav.paw.dolly.api.producer

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.dolly.api.config.ApplicationConfig
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HendelseKafkaProducer(
    private val applicationConfig: ApplicationConfig,
    private val producer: Producer<Long, Hendelse>
) {
    private val logger = buildLogger

    fun sendHendelse(key: Long, message: Hendelse) = runBlocking {
        val topic = applicationConfig.kafkaTopology.hendelsesloggTopic
        val record = ProducerRecord(topic, key, message)
        val metadata = producer.sendDeferred(record).await()
        logger.debug(
            "Sender melding til Kafka topic {} (partition={}, offset={})",
            topic,
            metadata.partition(),
            metadata.offset()
        )
    }
}