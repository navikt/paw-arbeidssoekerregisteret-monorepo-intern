package no.nav.paw.kafkakeymaintenance.kafka

import java.time.Instant

@JvmInline
value class Topic(val value: String)
fun topic(value: String): Topic = Topic(value)

@JvmInline
value class PartitionTime(val value: Instant)
fun partitionTime(value: Instant): PartitionTime = PartitionTime(value)

@JvmInline
value class PartitionLastUpdated(val value: Instant)
fun partitionLastUpdated(value: Instant): PartitionLastUpdated = PartitionLastUpdated(value)

fun hwmInfo(topic: Topic, partition: Int, offset: Long): HwmInfo = Hwm(
    topic = topic,
    partition = partition,
    offset = offset
)

fun topicPartitionMetadata(
    topic: Topic,
    partition: Int,
    offset: Long,
    time: PartitionTime,
    lastUpdated: PartitionLastUpdated
): TopicPartitionMetadata = topicPartitionMetadata(
    topic = topic,
    partition = partition,
    offset = offset,
    time = time,
    lastUpdated = lastUpdated
)

fun progressInfo(time: PartitionTime, lastUpdated: PartitionLastUpdated): ProgressInfo =
    SimpleProgressInfo(time, lastUpdated)

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

private class SimpleProgressInfo(
    override val time: PartitionTime,
    override val lastUpdated: PartitionLastUpdated
): ProgressInfo
