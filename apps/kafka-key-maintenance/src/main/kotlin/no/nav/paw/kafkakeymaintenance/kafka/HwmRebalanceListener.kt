package no.nav.paw.kafkakeymaintenance.kafka

import no.nav.paw.kafkakeymaintenance.ApplicationContext
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

class HwmRebalanceListener(
    private val contextFactory: Transaction.() -> TransactionContext,
    private val context: ApplicationContext,
    private val consumer: Consumer<*, *>
) : ConsumerRebalanceListener {

    private val currentPartitions = ConcurrentHashMap<Int, TopicPartition>(6)

    val currentlyAssignedPartitions: Set<Int> get() = currentPartitions.keys

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        context.logger.info("Revoked: $partitions")
        partitions?.forEach { partition ->
            currentPartitions.remove(partition.partition())
        }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        partitions?.forEach { partition ->
            currentPartitions.putIfAbsent(partition.partition(), partition)
        }
        val assignedPartitions = partitions ?: emptyList()
        context.logger.info("Assigned partitions $assignedPartitions")
        if (assignedPartitions.isNotEmpty()) {
            val seekTo = transaction {
                val txContext = contextFactory()
                assignedPartitions.map { metadata ->
                    val topic = Topic(metadata.topic())
                    val partition = metadata.partition()
                    val offset = requireNotNull(txContext.getHwm(topic, partition)) {
                        "No hwm for partition ${partition}, init not called?"
                    }
                    metadata to offset
                }
            }
            seekTo.forEach { (partition, offset) ->
                context.logger.info("Seeking to $partition, $offset")
                consumer.seek(partition, offset + 1)
            }
        }
    }
}