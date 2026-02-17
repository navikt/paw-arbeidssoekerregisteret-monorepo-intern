package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object BestilteVarslerTable : Table("bestilte_varsler") {
    val bestillingId = javaUUID("bestilling_id").references(BestillingerTable.bestillingId)
    val periodeId = javaUUID("periode_id")
    val varselId = javaUUID("varsel_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val status = enumerationByName<BestiltVarselStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)
}