package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseRow
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseStatus
import no.nav.paw.kafkakeygenerator.database.IdentitetHendelseTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetHendelseRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.updateReturning
import java.time.Instant

class IdentitetHendelseRepository {
    fun findById(
        id: Long
    ): IdentitetHendelseRow? = transaction {
        IdentitetHendelseTable.selectAll()
            .where { IdentitetHendelseTable.id eq id }
            .orderBy(IdentitetHendelseTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asIdentitetHendelseRow() }
            .singleOrNull()
    }

    fun findByAktorId(
        aktorId: String
    ): List<IdentitetHendelseRow> = transaction {
        IdentitetHendelseTable.selectAll()
            .where { IdentitetHendelseTable.aktorId eq aktorId }
            .orderBy(IdentitetHendelseTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asIdentitetHendelseRow() }
    }

    fun insert(
        arbeidssoekerId: Long,
        aktorId: String,
        data: String,
        status: IdentitetHendelseStatus
    ): Int = transaction {
        IdentitetHendelseTable.insert {
            it[IdentitetHendelseTable.arbeidssoekerId] = arbeidssoekerId
            it[IdentitetHendelseTable.aktorId] = aktorId
            it[IdentitetHendelseTable.data] = data
            it[IdentitetHendelseTable.status] = status
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateStatusById(
        id: Long,
        status: IdentitetHendelseStatus
    ): Int = transaction {
        IdentitetHendelseTable.update(where = {
            (IdentitetHendelseTable.id eq id)
        }) {
            it[IdentitetHendelseTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByStatusReturning(
        fraStatus: IdentitetHendelseStatus,
        tilStatus: IdentitetHendelseStatus
    ): List<Long> = transaction {
        IdentitetHendelseTable.updateReturning(
            returning = listOf(IdentitetHendelseTable.id),
            where = {
                IdentitetHendelseTable.status eq fraStatus
            }) {
            it[IdentitetHendelseTable.status] = tilStatus
            it[updatedTimestamp] = Instant.now()
        }.map {
            it[IdentitetHendelseTable.id].value
        }
    }
}