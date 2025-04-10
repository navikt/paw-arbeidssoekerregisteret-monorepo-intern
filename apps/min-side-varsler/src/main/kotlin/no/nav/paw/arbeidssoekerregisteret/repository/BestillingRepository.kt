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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class BestillingRepository {

    @WithSpan("BestillingRepository.findAll")
    fun findAll(paging: Paging = Paging.none()): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .orderBy(BestillingerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestillingRow() }
    }

    @WithSpan("BestillingRepository.findAll")
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

    @WithSpan("BestillingRepository.findByBestillingId")
    fun findByBestillingId(bestillingId: UUID): BestillingRow? = transaction {
        BestillingerTable.selectAll()
            .where { BestillingerTable.bestillingId eq bestillingId }
            .map { it.asBestillingRow() }
            .singleOrNull()
    }

    @WithSpan("BestillingRepository.findByUpdatedTimestampAndStatus")
    fun findByUpdatedTimestampAndStatus(
        updateTimestamp: Instant,
        vararg status: BestillingStatus
    ): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .where {
                (BestillingerTable.updatedTimestamp less updateTimestamp) and
                        (BestillingerTable.status inList status.toList())
            }
            .map { it.asBestillingRow() }
    }

    @WithSpan("BestillingRepository.insert")
    fun insert(bestilling: InsertBestillingRow): Int = transaction {
        BestillingerTable.insert {
            it[bestillingId] = bestilling.bestillingId
            it[bestiller] = bestilling.bestiller
            it[status] = BestillingStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("BestillingRepository.update")
    fun update(bestilling: UpdateBestillingRow): Int = transaction {
        BestillingerTable.update({
            BestillingerTable.bestillingId eq bestilling.bestillingId
        }) {
            it[status] = bestilling.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    @WithSpan("BestillingRepository.deleteByBestillingId")
    fun deleteByBestillingId(bestillingId: UUID): Int = transaction {
        BestillingerTable.deleteWhere { BestillingerTable.bestillingId eq bestillingId }
    }
}