package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object BestillingerTable : Table("bestillinger") {
    val bestillingId = uuid("bestilling_id")
    val bestiller = varchar("bestiller", 50)
    val status = enumerationByName<BestillingStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(bestillingId)
}