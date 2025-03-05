package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object VarslerTable : Table("varsler") {
    val periodeId = uuid("periode_id")
    val varselId = uuid("varsel_id")
    val varselKilde = enumerationByName<VarselKilde>("varsel_kilde", 50)
    val varselType = enumerationByName<VarselType>("varsel_type", 50)
    val varselStatus = enumerationByName<VarselStatus>("varsel_status", 50)
    val hendelseNavn = enumerationByName<VarselEventName>("hendelse_navn", 50)
    val hendelseTimestamp = timestamp("hendelse_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)
}