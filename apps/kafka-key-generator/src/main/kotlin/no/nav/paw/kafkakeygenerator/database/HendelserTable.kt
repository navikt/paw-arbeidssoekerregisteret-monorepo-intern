package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object HendelserTable : LongIdTable("hendelser") {
    val arbeidssoekerId = long("arbeidssoeker_id").references(KafkaKeysTable.id)
    val aktorId = varchar("aktor_id", 50)
    val version = integer("version")
    val data = jsonb("data")
    val partition = integer("kafka_partition").nullable()
    val offset = long("kafka_offset").nullable()
    val status = enumerationByName<HendelseStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun jsonb(name: String) = registerColumn(name, JsonbColumnType(false))
}