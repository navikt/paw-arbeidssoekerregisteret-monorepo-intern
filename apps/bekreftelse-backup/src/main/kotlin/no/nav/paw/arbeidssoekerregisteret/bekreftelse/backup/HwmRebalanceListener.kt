package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

class HwmRebalanceListener(
    private val context: ApplicationContext,
    private val consumer: Consumer<*, *>,
    private val topic: String
) : ConsumerRebalanceListener {

    private val currentPartitions = ConcurrentHashMap<Int, TopicPartition>(6)

    val currentlyAssignedPartitions: Set<Int> get() = currentPartitions.keys

    private val txContext = txContext(context)

    override fun onPartitionsRevoked(partitions: MutableCollection<TopicPartition>?) {
        context.logger.info("Revoked: $partitions for topic $topic")
        partitions?.forEach { partition ->
            currentPartitions.remove(partition.partition())
        }
    }

    override fun onPartitionsAssigned(partitions: MutableCollection<TopicPartition>?) {
        partitions?.forEach { partition ->
            currentPartitions.putIfAbsent(partition.partition(), partition)
        }
        val assignedPartitions = partitions ?: emptyList()
        context.logger.info("Assigned partitions $assignedPartitions for topic $topic")
        if (assignedPartitions.isNotEmpty()) {
            val seekTo = transaction {
                assignedPartitions.map { partition ->
                    val offset = requireNotNull(txContext().getHwm(partition.partition(), topic)) {
                        "No hwm for partition ${partition.partition()} on topic $topic, init not called?"
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