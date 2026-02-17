package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object PerioderTable : Table("perioder") {
    val periodeId = javaUUID("periode_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val startetTimestamp = timestamp("startet_timestamp")
    val avsluttetTimestamp = timestamp("avsluttet_timestamp").nullable()
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(periodeId)
}