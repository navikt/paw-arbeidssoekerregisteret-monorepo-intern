package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseRow
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetHendelseRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class IdentitetHendelseRepository {
    fun findByAktorId(
        aktorId: String
    ): List<IdentitetHendelseRow> = transaction {
        IdentitetHendelseTable.selectAll()
            .where { IdentitetHendelseTable.aktorId eq aktorId }
            .orderBy(IdentitetHendelseTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asIdentitetHendelseRow() }
    }

    fun findByStatus(
        status: IdentitetHendelseStatus
    ): List<IdentitetHendelseRow> = transaction {
        IdentitetHendelseTable.selectAll()
            .where { IdentitetHendelseTable.status eq status }
            .orderBy(IdentitetHendelseTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asIdentitetHendelseRow() }
    }

    fun findByStatusForUpdate(
        status: IdentitetHendelseStatus,
        limit: Int = Int.MAX_VALUE
    ): List<IdentitetHendelseRow> = transaction {
        IdentitetHendelseTable.selectAll()
            .forUpdate()
            .where { IdentitetHendelseTable.status eq status }
            .orderBy(IdentitetHendelseTable.insertedTimestamp, SortOrder.ASC)
            .limit(limit)
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

    fun updateStatusByIdList(
        idList: List<Long>,
        status: IdentitetHendelseStatus
    ): Int = transaction {
        IdentitetHendelseTable.update(where = {
            (IdentitetHendelseTable.id inList idList)
        }) {
            it[IdentitetHendelseTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }
}