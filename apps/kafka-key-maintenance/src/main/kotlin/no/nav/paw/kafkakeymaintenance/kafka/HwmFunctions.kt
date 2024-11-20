package no.nav.paw.kafkakeymaintenance.kafka

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import java.time.Instant

data class TransactionContext(
    val consumerVersion: Int,
    val transaction: Transaction
)

fun txContext(consumerVersion: Int): Transaction.() -> TransactionContext = {
    TransactionContext(consumerVersion, this)
}

fun TransactionContext.initHwm(topic: Topic, partitionCount: Int) {
    (0 until partitionCount)
        .filter { getHwm(topic, it) == null }
        .forEach { insertHwm(topic, it, -1) }
}

fun TransactionContext.getHwm(topic: Topic, partition: Int): Long? =
    HwmTable
        .selectAll()
        .where {
            (HwmTable.topic eq topic.value) and
                    (HwmTable.partition eq partition) and
                    (HwmTable.version eq consumerVersion)
        }
        .singleOrNull()?.get(HwmTable.offset)

fun TransactionContext.getTopicPartitionMetadata(topic: Topic, partition: Int): TopicPartitionMetadata? =
    HwmTable
        .selectAll()
        .where {
            (HwmTable.topic eq topic.value) and
                    (HwmTable.partition eq partition) and
                    (HwmTable.version eq consumerVersion)
        }
        .singleOrNull()
        ?.let {
            TopicPartitionMetadata(
                topic = Topic(it[HwmTable.topic]),
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset],
                time = PartitionTime(it[HwmTable.time]),
                lastUpdated = PartitionLastUpdated(it[HwmTable.lastUpdated])
            )
        }

fun TransactionContext.getAllHwms(): List<HwmInfo> =
    HwmTable
        .selectAll()
        .where { HwmTable.version eq consumerVersion }
        .map {
            hwmInfo(
                topic = Topic(it[HwmTable.topic]),
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

fun TransactionContext.insertHwm(
    topic: Topic,
    partition: Int,
    offset: Long,
    time: Instant = Instant.EPOCH,
    lastUpdated: Instant = Instant.EPOCH
) {
    HwmTable.insert {
        it[HwmTable.topic] = topic.value
        it[version] = consumerVersion
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
        it[HwmTable.time] = time
        it[HwmTable.lastUpdated] = lastUpdated
    }
}

fun TransactionContext.updateHwm(
    topic: Topic,
    partition: Int,
    offset: Long,
    time: Instant,
    lastUpdated: Instant
): Boolean =
    HwmTable
        .update({
            (HwmTable.topic eq topic.value) and
                    (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq consumerVersion)
        }
        ) {
            it[HwmTable.offset] = offset
            it[HwmTable.time] = HwmTable.time.max(time)
            it[HwmTable.lastUpdated] = lastUpdated
        } == 1

infix fun <T> ExpressionWithColumnType<T>.max(t: T): Greatest<T> = Greatest(
    expr1 = this,
    expr2 = wrap(t),
    columnType = this.columnType
)

class Greatest<T>(
    expr1: Expression<T>,
    expr2: Expression<T>,
    columnType: IColumnType<T & Any>
) : CustomFunction<T>(
    functionName = "greatest",
    columnType = columnType,
    expr1,
    expr2
)
