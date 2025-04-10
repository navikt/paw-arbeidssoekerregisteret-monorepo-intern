package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestilteVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import no.nav.paw.arbeidssoekerregisteret.model.asVarselId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class BestiltVarselRepository {

    @WithSpan("BestiltVarselRepository.findAll")
    fun findAll(paging: Paging = Paging.none()): List<BestiltVarselRow> = transaction {
        BestilteVarslerTable.selectAll()
            .orderBy(BestilteVarslerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestiltVarselRow() }
    }

    @WithSpan("BestiltVarselRepository.findByVarselId")
    fun findByVarselId(varselId: UUID): BestiltVarselRow? = transaction {
        BestilteVarslerTable.selectAll()
            .where { BestilteVarslerTable.varselId eq varselId }
            .map { it.asBestiltVarselRow() }
            .firstOrNull()
    }

    @WithSpan("BestiltVarselRepository.findByBestillingId")
    fun findByBestillingId(
        bestillingId: UUID,
        paging: Paging = Paging.none(),
    ): List<BestiltVarselRow> = transaction {
        BestilteVarslerTable.selectAll()
            .where { BestilteVarslerTable.bestillingId eq bestillingId }
            .orderBy(BestilteVarslerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestiltVarselRow() }
    }

    @WithSpan("BestiltVarselRepository.findVarselIdByBestillingId")
    fun findVarselIdByBestillingIdAndStatus(
        bestillingId: UUID,
        status: BestiltVarselStatus,
        paging: Paging = Paging.none(),
    ): List<UUID> = transaction {
        BestilteVarslerTable.selectAll()
            .where { (BestilteVarslerTable.bestillingId eq bestillingId) and (BestilteVarslerTable.status eq status) }
            .orderBy(BestilteVarslerTable.insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselId() }
    }

    @WithSpan("BestiltVarselRepository.insert")
    fun insert(varsel: InsertBestiltVarselRow): Int = transaction {
        BestilteVarslerTable.insert {
            it[bestillingId] = varsel.bestillingId
            it[periodeId] = varsel.periodeId
            it[varselId] = varsel.varselId
            it[identitetsnummer] = varsel.identitetsnummer
            it[status] = BestiltVarselStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("BestiltVarselRepository.update")
    fun update(varsel: UpdateBestiltVarselRow): Int = transaction {
        BestilteVarslerTable.update({
            BestilteVarslerTable.varselId eq varsel.varselId
        }) {
            it[status] = varsel.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    @WithSpan("BestiltVarselRepository.deleteByBestillingIdAndUpdatedTimestampAndStatus")
    fun deleteByBestillingIdAndUpdatedTimestampAndStatus(
        bestillingId: UUID,
        updateTimestamp: Instant,
        vararg status: BestiltVarselStatus
    ): Int = transaction {
        BestilteVarslerTable.deleteWhere {
            (BestilteVarslerTable.bestillingId eq bestillingId) and
                    (BestilteVarslerTable.updatedTimestamp less updateTimestamp) and
                    (BestilteVarslerTable.status inList status.toList())
        }
    }

    @WithSpan("BestiltVarselRepository.countByBestillingId")
    fun countByBestillingId(bestillingId: UUID): Long = transaction {
        BestilteVarslerTable.selectAll()
            .where { BestilteVarslerTable.bestillingId eq bestillingId }
            .count()
    }

    @WithSpan("BestiltVarselRepository.countByBestillingIdAndStatus")
    fun countByBestillingIdAndStatus(
        bestillingId: UUID,
        status: BestiltVarselStatus
    ): Long = transaction {
        BestilteVarslerTable.selectAll()
            .where { (BestilteVarslerTable.bestillingId eq bestillingId) and (BestilteVarslerTable.status eq status) }
            .count()
    }

    @WithSpan("BestiltVarselRepository.insertAktivePerioder")
    fun insertAktivePerioder(bestillingId: UUID) = transaction {
        val status = BestiltVarselStatus.VENTER.name
        exec(
            """
            INSERT INTO bestilte_varsler (
                bestilling_id,
                periode_id,
                varsel_id,
                identitetsnummer,
                status,
                inserted_timestamp
            )
            SELECT '$bestillingId',
                   periode_id,
                   gen_random_uuid(),
                   identitetsnummer,
                   '$status',
                   now()
            FROM perioder
            WHERE avsluttet_timestamp IS NULL
        """.trimIndent()
        )
    }
}