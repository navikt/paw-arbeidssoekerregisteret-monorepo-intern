package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object IdentiteterTable : LongIdTable("identiteter") {
    val arbeidssoekerId = long("arbeidssoeker_id").references(KafkaKeysTable.id)
    val aktorId = varchar("aktor_id", 50)
    val identitet = varchar("identitet", 50)
    val type = enumerationByName<IdentitetType>("type", 50)
    val gjeldende = bool("gjeldende")
    val status = enumerationByName<IdentitetStatus>("status", 50)
    val sourceTimestamp = timestamp("source_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}