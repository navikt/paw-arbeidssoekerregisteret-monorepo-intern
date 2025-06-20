package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object KonflikterTable : LongIdTable("konflikter") {
    val aktorId = varchar("aktor_id", 50)
    val type = enumerationByName<KonfliktType>("type", 50)
    val status = enumerationByName<KonfliktStatus>("status", 50)
    val sourceTimestamp = timestamp("source_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}