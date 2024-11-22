package no.nav.paw.kafkakeymaintenance.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

interface HwmRunnerProcessor<K, V> {
    fun ignore(record: ConsumerRecord<K, V>): Boolean
    fun process(txContext: TransactionContext, record: ConsumerRecord<K, V>): Unit
}