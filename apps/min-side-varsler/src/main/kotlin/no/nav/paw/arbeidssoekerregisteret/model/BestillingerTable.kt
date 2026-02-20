package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object BestillingerTable : Table("bestillinger") {
    val bestillingId = javaUUID("bestilling_id")
    val bestiller = varchar("bestiller", 50)
    val status = enumerationByName<BestillingStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(bestillingId)

    fun findAll(paging: Paging = Paging.none()): List<BestillingRow> = transaction {
        selectAll()
            .orderBy(insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestillingRow() }
    }

    fun findByStatus(
        status: BestillingStatus,
        paging: Paging = Paging.none()
    ): List<BestillingRow> = transaction {
        selectAll()
            .where { BestillingerTable.status eq status }
            .orderBy(insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestillingRow() }
    }

    fun findByBestillingId(bestillingId: UUID): BestillingRow? = transaction {
        selectAll()
            .where { BestillingerTable.bestillingId eq bestillingId }
            .map { it.asBestillingRow() }
            .singleOrNull()
    }

    fun insert(bestilling: InsertBestillingRow): Int = transaction {
        insert {
            it[bestillingId] = bestilling.bestillingId
            it[bestiller] = bestilling.bestiller
            it[status] = BestillingStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(bestilling: UpdateBestillingRow): Int = transaction {
        update({
            bestillingId eq bestilling.bestillingId
        }) {
            it[status] = bestilling.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByBestillingId(bestillingId: UUID): Int = transaction {
        deleteWhere { BestillingerTable.bestillingId eq bestillingId }
    }
}