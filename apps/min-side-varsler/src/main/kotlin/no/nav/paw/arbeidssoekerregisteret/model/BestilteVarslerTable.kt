package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object BestilteVarslerTable : Table("bestilte_varsler") {
    val bestillingId = uuid("bestilling_id").references(BestillingerTable.bestillingId)
    val periodeId = uuid("periode_id")
    val varselId = uuid("varsel_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val status = enumerationByName<BestiltVarselStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)
}