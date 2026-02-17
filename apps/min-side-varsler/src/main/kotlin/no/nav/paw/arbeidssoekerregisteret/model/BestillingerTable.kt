package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object BestillingerTable : Table("bestillinger") {
    val bestillingId = javaUUID("bestilling_id")
    val bestiller = varchar("bestiller", 50)
    val status = enumerationByName<BestillingStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(bestillingId)
}