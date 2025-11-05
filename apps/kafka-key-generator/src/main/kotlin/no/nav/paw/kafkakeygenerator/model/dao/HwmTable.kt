package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.kafkakeygenerator.utils.max
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant

object HwmTable : Table("hwm") {
    val version = integer("version")
    val topic = varchar("kafka_topic", 255)
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val timestamp = timestamp("timestamp")
    val updatedTimestamp = timestamp("updated_timestamp")
    override val primaryKey: PrimaryKey = PrimaryKey(version, topic, partition)

    fun getByTopicAndPartition(
        version: Int,
        topic: String,
        partition: Int
    ): HwmRow? = selectAll()
        .where {
            (HwmTable.version eq version) and
                    (HwmTable.topic eq topic) and
                    (HwmTable.partition eq partition)
        }
        .map { it.asHwmRow() }
        .singleOrNull()

    fun findByTopic(
        version: Int,
        topic: String
    ): List<HwmRow> = selectAll()
        .where {
            (HwmTable.version eq version) and (HwmTable.topic eq topic)
        }
        .map { it.asHwmRow() }

    fun findAll(
        version: Int
    ): List<HwmRow> = selectAll()
        .where { HwmTable.version eq version }
        .map { it.asHwmRow() }

    fun insert(
        version: Int,
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Instant = Instant.EPOCH,
        updatedTimestamp: Instant = Instant.EPOCH
    ): Int = insert {
        it[HwmTable.topic] = topic
        it[HwmTable.version] = version
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
        it[HwmTable.timestamp] = timestamp
        it[HwmTable.updatedTimestamp] = updatedTimestamp
    }.insertedCount

    fun update(
        version: Int,
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Instant,
        updatedTimestamp: Instant
    ): Int = update({
        (HwmTable.version eq version) and
                (HwmTable.topic eq topic) and
                (HwmTable.partition eq partition) and
                (HwmTable.offset less offset)
    }) {
        it[HwmTable.offset] = offset
        it[HwmTable.timestamp] = HwmTable.timestamp.max(timestamp)
        it[HwmTable.updatedTimestamp] = updatedTimestamp
    }
}