package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object EksterneVarslerTable : Table("eksterne_varsler") {
    val varselId = javaUUID("varsel_id").references(VarslerTable.varselId)
    val varselType = enumerationByName<VarselType>("varsel_type", 50)
    val varselStatus = enumerationByName<VarselStatus>("varsel_status", 50)
    val hendelseNavn = enumerationByName<VarselEventName>("hendelse_navn", 50)
    val hendelseTimestamp = timestamp("hendelse_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)

    fun findAll(paging: Paging = Paging.none()): List<EksterntVarselRow> = transaction {
        selectAll()
            .orderBy(hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asEksterntVarselRow() }
    }

    fun findByVarselId(varselId: UUID): EksterntVarselRow? = transaction {
        selectAll()
            .where { EksterneVarslerTable.varselId eq varselId }
            .map { it.asEksterntVarselRow() }
            .firstOrNull()
    }

    fun insert(varsel: InsertEksterntVarselRow): Int = transaction {
        insert {
            it[varselId] = varsel.varselId
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(varsel: UpdateEksterntVarselRow): Int = transaction {
        EksterneVarslerTable.update({
            varselId eq varsel.varselId
        }) {
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}