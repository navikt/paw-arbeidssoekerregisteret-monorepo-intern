package no.nav.paw.kafkakeygenerator.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PerioderTable : Table("perioder") {
    val periodeId = uuid("periode_id")
    val identitet = varchar("identitet", 50)
    val startetTimestamp = timestamp("startet_timestamp")
    val avsluttetTimestamp = timestamp("avsluttet_timestamp").nullable()
    val sourceTimestamp = timestamp("source_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}