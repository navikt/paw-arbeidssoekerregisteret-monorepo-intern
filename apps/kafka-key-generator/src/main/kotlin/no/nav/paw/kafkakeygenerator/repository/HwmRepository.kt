package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.HwmRow
import no.nav.paw.kafkakeygenerator.database.HwmTable
import no.nav.paw.kafkakeygenerator.model.asHwmRow
import no.nav.paw.kafkakeygenerator.utils.max
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant

class HwmRepository {

    fun getByTopicAndPartition(
        version: Int,
        topic: String,
        partition: Int
    ): HwmRow? {
        return HwmTable
            .selectAll()
            .where {
                (HwmTable.version eq version) and
                        (HwmTable.topic eq topic) and
                        (HwmTable.partition eq partition)
            }
            .map { it.asHwmRow() }
            .singleOrNull()
    }

    fun findByTopic(
        version: Int,
        topic: String
    ): List<HwmRow> {
        return HwmTable
            .selectAll()
            .where {
                (HwmTable.version eq version) and (HwmTable.topic eq topic)
            }
            .map { it.asHwmRow() }
    }

    fun findAll(version: Int): List<HwmRow> {
        return HwmTable
            .selectAll()
            .where { HwmTable.version eq version }
            .map { it.asHwmRow() }
    }

    fun insert(
        version: Int,
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Instant = Instant.EPOCH,
        updatedTimestamp: Instant = Instant.EPOCH
    ): Int {
        return HwmTable.insert {
            it[HwmTable.topic] = topic
            it[HwmTable.version] = version
            it[HwmTable.partition] = partition
            it[HwmTable.offset] = offset
            it[HwmTable.timestamp] = timestamp
            it[HwmTable.updatedTimestamp] = updatedTimestamp
        }.insertedCount
    }

    fun update(
        version: Int,
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Instant,
        updatedTimestamp: Instant
    ): Int {
        return HwmTable
            .update({
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
}