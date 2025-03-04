package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.InsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselTable
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

    fun findAll(): List<VarselRow> = transaction {
        VarselTable.selectAll()
            .map { it.asVarselRow() }
    }

    fun findByVarselId(varselId: UUID): VarselRow? = transaction {
        VarselTable.selectAll()
            .where { VarselTable.varselId eq varselId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    fun findByPeriodeIdAndVarselKilde(
        periodeId: UUID,
        varselKilde: VarselKilde
    ): List<VarselRow> = transaction {
        VarselTable.selectAll()
            .where { (VarselTable.periodeId eq periodeId) and (VarselTable.varselKilde eq varselKilde) }
            .map { it.asVarselRow() }
    }

    fun insert(varsel: InsertVarselRow): Int = transaction {
        VarselTable.insert {
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
        VarselTable.update({
            VarselTable.varselId eq varsel.varselId
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
        VarselTable.deleteWhere { (VarselTable.periodeId eq periodeId) and (VarselTable.varselKilde eq varselKilde) }
    }

    fun deleteByVarselId(varselId: UUID): Int = transaction {
        VarselTable.deleteWhere { VarselTable.varselId eq varselId }
    }
}