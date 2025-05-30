package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.PeriodeRow
import no.nav.paw.kafkakeygenerator.database.PerioderTable
import no.nav.paw.kafkakeygenerator.model.asPeriodeRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class PeriodeRepository {
    fun getByPeriodeId(periodeId: UUID): PeriodeRow? = transaction {
        PerioderTable.selectAll()
            .where { PerioderTable.periodeId eq periodeId }
            .map { it.asPeriodeRow() }
            .singleOrNull()
    }

    fun findByIdentiteter(identitetList: List<String>): List<PeriodeRow> = transaction {
        PerioderTable.selectAll()
            .where { PerioderTable.identitet inList identitetList }
            .map { it.asPeriodeRow() }
    }

    fun insert(
        periodeId: UUID,
        identitet: String,
        startetTimestamp: Instant,
        avsluttetTimestamp: Instant? = null,
        sourceTimestamp: Instant
    ): Int = transaction {
        PerioderTable.insert {
            it[PerioderTable.periodeId] = periodeId
            it[PerioderTable.identitet] = identitet
            it[PerioderTable.startetTimestamp] = startetTimestamp
            it[PerioderTable.avsluttetTimestamp] = avsluttetTimestamp
            it[PerioderTable.sourceTimestamp] = sourceTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateAvsluttetTimestamp(
        periodeId: UUID,
        avsluttetTimestamp: Instant?,
        sourceTimestamp: Instant
    ): Int = transaction {
        PerioderTable.update(where = {
            (PerioderTable.periodeId eq periodeId)
        }) {
            it[PerioderTable.avsluttetTimestamp] = avsluttetTimestamp
            it[PerioderTable.sourceTimestamp] = sourceTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}