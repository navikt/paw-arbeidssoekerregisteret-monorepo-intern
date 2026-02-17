package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.BestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestillingerTable
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.asBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

class BestillingRepository {

    @WithSpan("findAll")
    fun findAll(paging: Paging = Paging.none()): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .orderBy(BestillingerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestillingRow() }
    }

    @WithSpan("findAll")
    fun findByStatus(
        status: BestillingStatus,
        paging: Paging = Paging.none()
    ): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .where { BestillingerTable.status eq status }
            .orderBy(BestillingerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestillingRow() }
    }

    @WithSpan("findByBestillingId")
    fun findByBestillingId(bestillingId: UUID): BestillingRow? = transaction {
        BestillingerTable.selectAll()
            .where { BestillingerTable.bestillingId eq bestillingId }
            .map { it.asBestillingRow() }
            .singleOrNull()
    }

    @WithSpan("insert")
    fun insert(bestilling: InsertBestillingRow): Int = transaction {
        BestillingerTable.insert {
            it[bestillingId] = bestilling.bestillingId
            it[bestiller] = bestilling.bestiller
            it[status] = BestillingStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("update")
    fun update(bestilling: UpdateBestillingRow): Int = transaction {
        BestillingerTable.update({
            BestillingerTable.bestillingId eq bestilling.bestillingId
        }) {
            it[status] = bestilling.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    @WithSpan("deleteByBestillingId")
    fun deleteByBestillingId(bestillingId: UUID): Int = transaction {
        BestillingerTable.deleteWhere { BestillingerTable.bestillingId eq bestillingId }
    }
}