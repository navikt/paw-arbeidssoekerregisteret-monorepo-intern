package no.nav.paw.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.kafka.consumer")

fun <K, V> KafkaConsumer<K, V>.defaultSuccessFunction(records: ConsumerRecords<K, V>) {
    if (!records.isEmpty) {
        logger.debug("Kafka Consumer success. {} records processed", records.count())
        this.commitSync()
    }
}

fun defaultErrorFunction(throwable: Throwable) {
    logger.error("Kafka Consumer failed", throwable)
    throw throwable
}
