package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object KonfliktIdentiteterTable : LongIdTable("konflikt_identiteter") {
    val konfliktId = long("konflikt_id")
    val identitet = varchar("identitet", 50)
    val type = enumerationByName<IdentitetType>("type", 50)
    val gjeldende = bool("gjeldende")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
}