package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PeriodeTable : Table("perioder") {
    val periodeId = uuid("periode_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val startetTimestamp = timestamp("startet_timestamp")
    val avsluttetTimestamp = timestamp("avsluttet_timestamp").nullable()
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(periodeId)
}