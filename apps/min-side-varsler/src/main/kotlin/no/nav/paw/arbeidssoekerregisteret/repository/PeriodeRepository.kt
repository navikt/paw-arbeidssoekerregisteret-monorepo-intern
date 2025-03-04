package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.InsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.model.UpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class PeriodeRepository {

    fun findAll(): List<PeriodeRow> = transaction {
        PeriodeTable.selectAll()
            .map { it.asPeriodeRow() }
    }

    fun findByPeriodeId(periodeId: UUID): PeriodeRow? = transaction {
        PeriodeTable.selectAll()
            .where { PeriodeTable.periodeId eq periodeId }
            .map { it.asPeriodeRow() }
            .singleOrNull()
    }

    fun insert(periode: InsertPeriodeRow): Int = transaction {
        PeriodeTable.insert {
            it[periodeId] = periode.periodeId
            it[identitetsnummer] = periode.identitetsnummer
            it[startetTimestamp] = periode.startetTimestamp
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(periode: UpdatePeriodeRow): Int = transaction {
        PeriodeTable.update({
            PeriodeTable.periodeId eq periode.periodeId
        }) {
            it[identitetsnummer] = periode.identitetsnummer
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByPeriodeId(periodeId: UUID): Int = transaction {
        PeriodeTable.deleteWhere { PeriodeTable.periodeId eq periodeId }
    }
}