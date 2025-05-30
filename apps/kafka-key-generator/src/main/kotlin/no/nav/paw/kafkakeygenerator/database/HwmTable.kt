package no.nav.paw.kafkakeygenerator.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HwmTable : Table("hwm") {
    val version = integer("version")
    val topic = varchar("kafka_topic", 255)
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val timestamp = timestamp("timestamp")
    val updatedTimestamp = timestamp("updated_timestamp")
    override val primaryKey: PrimaryKey = PrimaryKey(version, topic, partition)
}