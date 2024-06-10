package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssoekerregisteret.backup.database.getHwm
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

class HwmRebalanceListener(
    private val context: ApplicationContext,
    private val consumer: Consumer<*, *>
) : ConsumerRebalanceListener {

    private val currentPartitions =  ConcurrentHashMap<Int, TopicPartition>(6)

    val currentlyAssignedPartitions: Set<Int> get() = currentPartitions.keys

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        context.logger.info("Revoked: $partitions")
        partitions?.forEach { partition ->
            currentPartitions.remove(partition.partition())
        }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        with(context) {
            partitions?.forEach { partition ->
                currentPartitions.putIfAbsent(partition.partition(), partition)
            }
            val assignedPartitions = partitions ?: emptyList()
            context.logger.info("Assigned partitions $assignedPartitions")
            if (assignedPartitions.isNotEmpty()) {
                val seekTo = transaction {
                    assignedPartitions.map { partition ->
                        val offset = requireNotNull(getHwm(partition.partition())) {
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
}