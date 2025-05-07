package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktRow
import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetKonflikterTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetKonfliktRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class IdentitetKonfliktRepository {
    fun findByAktorId(
        aktorId: String,
    ): List<IdentitetKonfliktRow> = transaction {
        IdentitetKonflikterTable.selectAll()
            .where { IdentitetKonflikterTable.aktorId eq aktorId }
            .map { it.asIdentitetKonfliktRow() }
    }

    fun findByStatusForUpdate(
        status: IdentitetKonfliktStatus,
        limit: Int = Int.MAX_VALUE
    ): List<IdentitetKonfliktRow> = transaction {
        IdentitetKonflikterTable.selectAll()
            .forUpdate()
            .where { IdentitetKonflikterTable.status eq status }
            .orderBy(IdentitetKonflikterTable.insertedTimestamp, SortOrder.ASC)
            .limit(limit)
            .map { it.asIdentitetKonfliktRow() }
    }

    fun insert(
        aktorId: String,
        status: IdentitetKonfliktStatus
    ): Int = transaction {
        IdentitetKonflikterTable.insert {
            it[IdentitetKonflikterTable.aktorId] = aktorId
            it[IdentitetKonflikterTable.status] = status
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateStatusByAktorId(
        aktorId: String,
        status: IdentitetKonfliktStatus
    ): Int = transaction {
        IdentitetKonflikterTable.update(where = {
            (IdentitetKonflikterTable.aktorId eq aktorId)
        }) {
            it[IdentitetKonflikterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }
}