package no.nav.paw.arbeidssoekerregisteret.repository

import no.nav.paw.arbeidssoekerregisteret.model.BestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestillingerTable
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.asBestillingRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class BestillingRepository {

    fun findAll(paging: Paging = Paging()): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .orderBy(BestillingerTable.insertedTimestamp, paging.ordering)
            .limit(paging.size).offset(paging.offset)
            .map { it.asBestillingRow() }
    }

    fun findByStatus(
        status: BestillingStatus,
        paging: Paging = Paging()
    ): List<BestillingRow> = transaction {
        BestillingerTable.selectAll()
            .where { BestillingerTable.status eq status }
            .orderBy(BestillingerTable.insertedTimestamp, paging.ordering)
            .limit(paging.size).offset(paging.offset)
            .map { it.asBestillingRow() }
    }

    fun findByBestillingId(bestillingId: UUID): BestillingRow? = transaction {
        BestillingerTable.selectAll()
            .where { BestillingerTable.bestillingId eq bestillingId }
            .map { it.asBestillingRow() }
            .singleOrNull()
    }

    fun insert(bestilling: InsertBestillingRow): Int = transaction {
        BestillingerTable.insert {
            it[bestillingId] = bestilling.bestillingId
            it[bestiller] = bestilling.bestiller
            it[status] = BestillingStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(bestilling: UpdateBestillingRow): Int = transaction {
        BestillingerTable.update({
            BestillingerTable.bestillingId eq bestilling.bestillingId
        }) {
            it[status] = bestilling.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByBestillingId(bestillingId: UUID): Int = transaction {
        BestillingerTable.deleteWhere { BestillingerTable.bestillingId eq bestillingId }
    }
}