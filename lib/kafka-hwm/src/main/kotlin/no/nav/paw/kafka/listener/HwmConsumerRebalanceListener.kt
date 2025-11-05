package no.nav.paw.kafka.listener

import no.nav.paw.kafka.config.KafkaConsumerConfig
import no.nav.paw.kafka.service.KafkaHwmOperations
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition

class HwmConsumerRebalanceListener(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val hwmOperations: KafkaHwmOperations,
    private val kafkaConsumer: KafkaConsumer<*, *>
) : ConsumerRebalanceListener {
    private val logger = buildNamedLogger("application.kafka.hwm")

    fun onPartitionsReady() {
        val partitionCount = kafkaConsumer.partitionsFor(kafkaConsumerConfig.topic).count()
        val initCount = hwmOperations.initHwm(kafkaConsumerConfig.topic, partitionCount)
        logger.info("Initialized HWM for {} of {} partitions", initCount, partitionCount)
    }

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.info("Revoked partitions {}", partitions)
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        val assignedPartitions = partitions ?: emptyList()
        logger.info("Assigned partitions {}", assignedPartitions)
        if (assignedPartitions.isEmpty()) {
            logger.warn("No partitions assigned")
        } else {
            val seekTo = assignedPartitions.map { partition ->
                val hwm = hwmOperations.getHwm(partition.topic(), partition.partition())
                partition to hwm.offset
            }
            seekTo.forEach { (partition, offset) ->
                logger.info("Seeking to HWM offset {} for partition {}", offset + 1, partition)
                kafkaConsumer.seek(partition, offset + 1)
            }
        }
    }
}