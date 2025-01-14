package no.nav.paw.bekreftelse.api.producer

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.utils.buildLogger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.producer.sendDeferred
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class BekreftelseKafkaProducer(
    private val applicationConfig: ApplicationConfig,
    private val producer: Producer<Long, Bekreftelse>
) {
    private val logger = buildLogger

    fun produceMessage(key: Long, message: Bekreftelse) = runBlocking {
        val topic = applicationConfig.kafkaTopology.bekreftelseTopic
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