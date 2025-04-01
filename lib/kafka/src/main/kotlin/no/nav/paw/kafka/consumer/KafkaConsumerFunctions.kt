package no.nav.paw.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.kafka.consumer")

fun <K, V> KafkaConsumer<K, V>.defaultSuccessFunction(records: ConsumerRecords<K, V>) {
    if (!records.isEmpty) {
        logger.debug("Kafka Consumer {} processed {} records", groupMetadata().memberId(), records.count())
        commitSync()
    }
}

fun KafkaConsumer<*, *>.defaultErrorFunction(throwable: Throwable) {
    val memberId = groupMetadata().memberId()
    logger.error("Kafka Consumer $memberId failed", throwable)
    throw throwable
}
