package no.nav.paw.arbeidssoeker.synk.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ArbeidssoekereSynkTable : Table("arbeidssoekere_synk") {
    val version = varchar("version", 20)
    val identitetsnummer = varchar("identitetsnummer", 20)
    val status = integer("status")
    val inserted = timestamp("inserted")
    val updated = timestamp("updated")
    override val primaryKey: PrimaryKey = PrimaryKey(version, identitetsnummer)
}