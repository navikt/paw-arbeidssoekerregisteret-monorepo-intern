package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.repository

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.InsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.UpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselRow
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.asVarselRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class VarselRepository {

    fun countAll(): Long = transaction {
        VarselTable.selectAll().count()
    }

    fun findByBekreftelseId(bekreftelseId: UUID): VarselRow? = transaction {
        VarselTable.selectAll()
            .where { VarselTable.bekreftelseId eq bekreftelseId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    fun findByPeriodeId(periodeId: UUID): List<VarselRow> = transaction {
        VarselTable.selectAll()
            .where { VarselTable.periodeId eq periodeId }
            .map { it.asVarselRow() }
    }

    fun insert(varsel: InsertVarselRow): Int = transaction {
        VarselTable.insert {
            it[periodeId] = varsel.periodeId
            it[bekreftelseId] = varsel.bekreftelseId
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(varsel: UpdateVarselRow): Int = transaction {
        VarselTable.update({
            VarselTable.bekreftelseId eq varsel.bekreftelseId
        }) {
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByPeriodeId(periodeId: UUID): Int = transaction {
        VarselTable.deleteWhere { VarselTable.periodeId eq periodeId }
    }

    fun deleteByBekreftelseId(bekreftelseId: UUID): Int = transaction {
        VarselTable.deleteWhere { VarselTable.bekreftelseId eq bekreftelseId }
    }
}