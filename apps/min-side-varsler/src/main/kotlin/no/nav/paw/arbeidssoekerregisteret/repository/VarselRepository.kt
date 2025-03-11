package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.InsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import no.nav.paw.arbeidssoekerregisteret.model.asVarselRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class VarselRepository {

    fun findAll(paging: Paging = Paging.none()): List<VarselRow> = transaction {
        VarslerTable.selectAll()
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun findByVarselId(varselId: UUID): VarselRow? = transaction {
        VarslerTable.selectAll()
            .where { VarslerTable.varselId eq varselId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    fun findByPeriodeId(
        periodeId: UUID,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        VarslerTable.selectAll()
            .where { VarslerTable.periodeId eq periodeId }
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun findByPeriodeIdAndVarselKilde(
        periodeId: UUID,
        varselKilde: VarselKilde,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        VarslerTable.selectAll()
            .where { (VarslerTable.periodeId eq periodeId) and (VarslerTable.varselKilde eq varselKilde) }
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    fun insert(varsel: InsertVarselRow): Int = transaction {
        VarslerTable.insert {
            it[periodeId] = varsel.periodeId
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
            VarslerTable.varselId eq varsel.varselId
        }) {
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByPeriodeIdAndVarselKilde(
        periodeId: UUID,
        varselKilde: VarselKilde
    ): Int = transaction {
        VarslerTable.deleteWhere { (VarslerTable.periodeId eq periodeId) and (VarslerTable.varselKilde eq varselKilde) }
    }

    fun deleteByVarselId(varselId: UUID): Int = transaction {
        VarslerTable.deleteWhere { VarslerTable.varselId eq varselId }
    }
}