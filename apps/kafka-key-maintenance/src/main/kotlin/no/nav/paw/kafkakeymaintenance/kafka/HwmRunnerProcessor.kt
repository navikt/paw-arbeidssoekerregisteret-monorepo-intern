package no.nav.paw.kafkakeymaintenance.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

interface HwmRunnerProcessor<K, V> {
    fun process(txContext: TransactionContext, record: ConsumerRecord<K, V>): Unit
}