package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.InsertPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.PerioderTable
import no.nav.paw.arbeidssoekerregisteret.model.UpdatePeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asPeriodeRow
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class PeriodeRepository {

    @WithSpan("findAll")
    fun findAll(paging: Paging = Paging.none()): List<PeriodeRow> = transaction {
        PerioderTable.selectAll()
            .orderBy(PerioderTable.startetTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asPeriodeRow() }
    }

    @WithSpan("findByPeriodeId")
    fun findByPeriodeId(periodeId: UUID): PeriodeRow? = transaction {
        PerioderTable.selectAll()
            .where { PerioderTable.periodeId eq periodeId }
            .map { it.asPeriodeRow() }
            .singleOrNull()
    }

    @WithSpan("insert")
    fun insert(periode: InsertPeriodeRow): Int = transaction {
        PerioderTable.insert {
            it[periodeId] = periode.periodeId
            it[identitetsnummer] = periode.identitetsnummer
            it[startetTimestamp] = periode.startetTimestamp
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("update")
    fun update(periode: UpdatePeriodeRow): Int = transaction {
        PerioderTable.update({
            PerioderTable.periodeId eq periode.periodeId
        }) {
            it[identitetsnummer] = periode.identitetsnummer
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}