package no.nav.paw.kafka.listener

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition

class NoopConsumerRebalanceListener : ConsumerRebalanceListener {
    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {}
    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {}
}