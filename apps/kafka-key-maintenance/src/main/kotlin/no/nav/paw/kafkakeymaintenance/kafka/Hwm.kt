package no.nav.paw.kafkakeymaintenance.kafka

import java.time.Instant

@JvmInline
value class Topic(val value: String)

@JvmInline
value class PartitionTime(val value: Instant)

@JvmInline
value class PartitionLastUpdated(val value: Instant)

fun hwmInfo(topic: Topic, partition: Int, offset: Long): HwmInfo = Hwm(
    topic = topic,
    partition = partition,
    offset = offset
)
interface HwmInfo {
    val topic: Topic
    val partition: Int
    val offset: Long
}

interface ProgressInfo {
    val time: PartitionTime
    val lastUpdated: PartitionLastUpdated
}

private data class Hwm(
    override val topic: Topic,
    override val partition: Int,
    override val offset: Long
): HwmInfo

data class TopicPartitionMetadata(
    override val topic: Topic,
    override val partition: Int,
    override val offset: Long,
    override val time: PartitionTime,
    override val lastUpdated: PartitionLastUpdated
): HwmInfo, ProgressInfo
