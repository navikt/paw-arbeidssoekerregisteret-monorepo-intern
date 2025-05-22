package no.nav.paw.arbeidssoekerregisteret.backup.kafka

import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getHwm
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class HwmRebalanceListener(
    private val consumerVersion: Int,
    private val consumer: Consumer<*, *>
) : ConsumerRebalanceListener {

    private val logger = LoggerFactory.getLogger(HwmRebalanceListener::class.java)
    private val currentPartitions = ConcurrentHashMap<Int, TopicPartition>(6)

    val currentlyAssignedPartitions: Set<Int> get() = currentPartitions.keys


    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        logger.info("Revoked: $partitions")
        partitions?.forEach { partition ->
            currentPartitions.remove(partition.partition())
        }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        partitions?.forEach { partition ->
            currentPartitions.putIfAbsent(partition.partition(), partition)
        }
        val assignedPartitions = partitions ?: emptyList()
        logger.info("Assigned partitions $assignedPartitions")
        if (assignedPartitions.isNotEmpty()) {
            val seekTo = transaction {
                assignedPartitions.map { partition ->
                    val offset = requireNotNull(getHwm(consumerVersion, partition.partition())) {
                        "No hwm for partition ${partition.partition()}, init not called?"
                    }
                    partition to offset
                }
            }
            seekTo.forEach { (partition, offset) ->
                consumer.seek(partition, offset + 1)
            }
        }
    }
}