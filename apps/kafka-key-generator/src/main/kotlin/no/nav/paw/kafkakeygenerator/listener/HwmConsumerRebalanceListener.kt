package no.nav.paw.kafkakeygenerator.listener

import no.nav.paw.kafkakeygenerator.config.KafkaConsumerConfig
import no.nav.paw.kafkakeygenerator.service.KafkaHwmOperations
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import java.util.concurrent.ConcurrentHashMap

class HwmConsumerRebalanceListener(
    private val kafkaConsumerConfig: KafkaConsumerConfig,
    private val hwmOperations: KafkaHwmOperations,
    private val kafkaConsumer: KafkaConsumer<*, *>
) : ConsumerRebalanceListener {
    private val logger = buildNamedLogger("application.kafka.hwm")
    private val currentPartitions = ConcurrentHashMap<Int, TopicPartition>(kafkaConsumerConfig.defaultPartitionCount)

    fun currentlyAssignedPartitions(): List<TopicPartition> {
        return currentPartitions.elements().toList()
    }

    fun onPartitionsReady() {
        val partitionCount = kafkaConsumer.partitionsFor(kafkaConsumerConfig.topic).count()
        val initCount = hwmOperations.initHwm(kafkaConsumerConfig.topic, partitionCount)
        logger.info("Initialiserte {} av {} partition HWMs", initCount, partitionCount)
    }

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.info("Revoked partitions {}", partitions)
        partitions?.forEach { partition ->
            currentPartitions.remove(partition.partition())
        }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        partitions?.forEach { partition ->
            currentPartitions.putIfAbsent(partition.partition(), partition)
        }
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
                logger.info("Seeking to offset {} for partition {}", offset + 1, partition)
                kafkaConsumer.seek(partition, offset + 1)
            }
        }
    }
}