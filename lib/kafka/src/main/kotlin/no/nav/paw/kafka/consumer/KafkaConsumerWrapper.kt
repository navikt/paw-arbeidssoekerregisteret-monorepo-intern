package no.nav.paw.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecords

interface KafkaConsumerWrapper<K, V> {

    fun init()

    fun consume(onConsume: (ConsumerRecords<K, V>) -> Unit)

    fun stop()
}