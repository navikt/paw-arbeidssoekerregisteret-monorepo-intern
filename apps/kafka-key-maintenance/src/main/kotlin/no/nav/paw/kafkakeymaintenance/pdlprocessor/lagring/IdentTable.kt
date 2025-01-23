package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import org.jetbrains.exposed.sql.Table

object IdentTable: Table("ident_table") {
    val ident = varchar("ident", 20)
    val identType = integer("ident_type")
    val gjeldende = bool("gjeldende")
    val personId = long("person_id").references(PersonTable.id)
    override val primaryKey = PrimaryKey(ident, identType, personId)
}