package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.KafkaKeysTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object IdentitetHendelseTable : LongIdTable("identitet_hendelser") {
    val arbeidssoekerId = long("arbeidssoeker_id").references(KafkaKeysTable.id)
    val aktorId = varchar("aktor_id", 50)
    val data = jsonb("data")
    val status = enumerationByName<IdentitetHendelseStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun jsonb(name: String) = registerColumn(name, JsonbColumnType(false))
}