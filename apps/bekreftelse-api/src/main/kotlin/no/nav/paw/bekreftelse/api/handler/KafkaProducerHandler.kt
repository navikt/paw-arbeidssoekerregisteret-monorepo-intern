package no.nav.paw.bekreftelse.api.handler

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaProducerHandler(
    private val applicationConfig: ApplicationConfig,
    private val kafkaProducer: Producer<Long, Bekreftelse>
) {
    private val logger = buildApplicationLogger

    fun sendBekreftelse(key: Long, value: Bekreftelse) = runBlocking {
        val topic = applicationConfig.kafkaTopology.bekreftelseTopic
        val metadata = kafkaProducer
            .sendDeferred(ProducerRecord(topic, key, value))
            .await()
        logger.debug(
            "Sender melding til Kafka topic {} (partition={}, offset={})",
            topic,
            metadata.partition(),
            metadata.offset()
        )
    }
}