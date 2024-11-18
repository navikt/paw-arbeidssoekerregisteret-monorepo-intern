package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object DataTable: Table("data") {
    val version = integer("version")
    val id = varchar("id", 255)
    val traceparent = binary("traceparant").nullable()
    val time = timestamp("time")
    val data = binary("data")
    override val primaryKey = PrimaryKey(id, version)
}