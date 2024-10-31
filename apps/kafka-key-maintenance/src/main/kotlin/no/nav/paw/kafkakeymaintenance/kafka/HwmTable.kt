package no.nav.paw.kafkakeymaintenance.kafka

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HwmTable: Table("hwm") {
    val version = integer("version")
    val topic = varchar("kafka_topic", 255)
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val time = timestamp("time")
    val lastUpdated = timestamp("last_updated")
    override val primaryKey: PrimaryKey = PrimaryKey(version, topic, partition)
}