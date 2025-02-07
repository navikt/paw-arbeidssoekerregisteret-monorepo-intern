package no.nav.paw.bekreftelse.api.utils

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

private val logger = buildApplicationLogger

fun <T> Producer<Long, T>.sendBlocking(topic: String, key: Long, message: T) = runBlocking {
    val metadata = sendDeferred(ProducerRecord(topic, key, message)).await()
    logger.debug(
        "Sender melding til Kafka topic {} (partition={}, offset={})",
        topic,
        metadata.partition(),
        metadata.offset()
    )
}
