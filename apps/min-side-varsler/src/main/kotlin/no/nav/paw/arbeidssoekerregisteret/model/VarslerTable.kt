package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object VarslerTable : Table("varsler") {
    val periodeId = javaUUID("periode_id")
    val bekreftelseId = javaUUID("bekreftelse_id").nullable()
    val varselId = javaUUID("varsel_id")
    val varselKilde = enumerationByName<VarselKilde>("varsel_kilde", 50)
    val varselType = enumerationByName<VarselType>("varsel_type", 50)
    val varselStatus = enumerationByName<VarselStatus>("varsel_status", 50)
    val hendelseNavn = enumerationByName<VarselEventName>("hendelse_navn", 50)
    val hendelseTimestamp = timestamp("hendelse_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)

    fun findAll(paging: Paging = Paging.none()): List<VarselRow> = transaction {
        join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .orderBy(hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun findByVarselId(varselId: UUID): VarselRow? = transaction {
        join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.varselId eq varselId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    fun findByBekreftelseId(bekreftelseId: UUID): VarselRow? = transaction {
        join(EksterneVarslerTable, JoinType.LEFT, varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.bekreftelseId eq bekreftelseId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    fun findByPeriodeId(
        periodeId: UUID,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.periodeId eq periodeId }
            .orderBy(hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun findByPeriodeIdAndVarselKilde(
        periodeId: UUID,
        varselKilde: VarselKilde,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { (VarslerTable.periodeId eq periodeId) and (VarslerTable.varselKilde eq varselKilde) }
            .orderBy(hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun insert(varsel: InsertVarselRow): Int = transaction {
        insert {
            it[periodeId] = varsel.periodeId
            it[bekreftelseId] = varsel.bekreftelseId
            it[varselId] = varsel.varselId
            it[varselKilde] = varsel.varselKilde
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(varsel: UpdateVarselRow): Int = transaction {
        VarslerTable.update({
            varselId eq varsel.varselId
        }) {
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}