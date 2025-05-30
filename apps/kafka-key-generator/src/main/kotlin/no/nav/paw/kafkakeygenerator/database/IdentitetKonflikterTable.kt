package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object IdentitetKonflikterTable : LongIdTable("identitet_konflikter") {
    val aktorId = varchar("aktor_id", 50)
    val status = enumerationByName<IdentitetKonfliktStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}