package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktRow
import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetKonflikterTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetKonfliktRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.updateReturning
import java.time.Instant

class IdentitetKonfliktRepository {
    fun getById(
        id: Long,
    ): IdentitetKonfliktRow? = transaction {
        IdentitetKonflikterTable.selectAll()
            .where { IdentitetKonflikterTable.id eq id }
            .map { it.asIdentitetKonfliktRow() }
            .singleOrNull()
    }

    fun findByAktorId(
        aktorId: String,
    ): List<IdentitetKonfliktRow> = transaction {
        IdentitetKonflikterTable.selectAll()
            .where { IdentitetKonflikterTable.aktorId eq aktorId }
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
            IdentitetKonflikterTable.aktorId eq aktorId
        }) {
            it[IdentitetKonflikterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByStatusReturning(
        fraStatus: IdentitetKonfliktStatus,
        tilStatus: IdentitetKonfliktStatus
    ): List<Long> = transaction {
        IdentitetKonflikterTable.updateReturning(
            returning = listOf(IdentitetKonflikterTable.id),
            where = {
                IdentitetKonflikterTable.status eq fraStatus
            }) {
            it[IdentitetKonflikterTable.status] = tilStatus
            it[updatedTimestamp] = Instant.now()
        }.map {
            it[IdentitetKonflikterTable.id].value
        }
    }
}