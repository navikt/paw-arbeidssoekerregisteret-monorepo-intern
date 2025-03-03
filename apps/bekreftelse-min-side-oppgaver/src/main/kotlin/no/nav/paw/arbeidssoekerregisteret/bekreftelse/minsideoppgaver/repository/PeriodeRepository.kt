package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.repository

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class PeriodeRepository {

    fun countAll(): Long = transaction {
        PeriodeTable.selectAll().count()
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