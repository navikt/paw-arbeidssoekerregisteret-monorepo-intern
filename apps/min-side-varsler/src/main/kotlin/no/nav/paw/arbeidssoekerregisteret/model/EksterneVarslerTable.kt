package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp

object EksterneVarslerTable : Table("eksterne_varsler") {
    val varselId = javaUUID("varsel_id").references(VarslerTable.varselId)
    val varselType = enumerationByName<VarselType>("varsel_type", 50)
    val varselStatus = enumerationByName<VarselStatus>("varsel_status", 50)
    val hendelseNavn = enumerationByName<VarselEventName>("hendelse_navn", 50)
    val hendelseTimestamp = timestamp("hendelse_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: Table.PrimaryKey = PrimaryKey(varselId)
}