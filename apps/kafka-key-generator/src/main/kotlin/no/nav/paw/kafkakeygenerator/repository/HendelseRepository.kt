package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.HendelserTable
import no.nav.paw.kafkakeygenerator.model.HendelseRow
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.model.asHendelseRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.updateReturning
import java.time.Instant

class HendelseRepository {
    fun findById(
        id: Long
    ): HendelseRow? = transaction {
        HendelserTable.selectAll()
            .where { HendelserTable.id eq id }
            .orderBy(HendelserTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asHendelseRow() }
            .singleOrNull()
    }

    fun findByAktorId(
        aktorId: String
    ): List<HendelseRow> = transaction {
        HendelserTable.selectAll()
            .where { HendelserTable.aktorId eq aktorId }
            .orderBy(HendelserTable.insertedTimestamp, SortOrder.ASC)
            .map { it.asHendelseRow() }
    }

    fun insert(
        arbeidssoekerId: Long,
        aktorId: String,
        version: Int,
        data: String,
        status: HendelseStatus
    ): Int = transaction {
        HendelserTable.insert {
            it[HendelserTable.arbeidssoekerId] = arbeidssoekerId
            it[HendelserTable.aktorId] = aktorId
            it[HendelserTable.version] = version
            it[HendelserTable.data] = data
            it[HendelserTable.status] = status
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateStatusById(
        id: Long,
        status: HendelseStatus
    ): Int = transaction {
        HendelserTable.update(where = {
            (HendelserTable.id eq id)
        }) {
            it[HendelserTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByStatusReturning(
        fraStatus: HendelseStatus,
        tilStatus: HendelseStatus,
        limit: Int = 1000
    ): List<Long> = transaction {
        val chunkQuery = HendelserTable.select(HendelserTable.id)
            .where { HendelserTable.status eq fraStatus }
            .orderBy(HendelserTable.id, SortOrder.ASC)
            .limit(limit)
        HendelserTable.updateReturning(
            returning = listOf(HendelserTable.id),
            where = { HendelserTable.id inSubQuery chunkQuery }) {
            it[HendelserTable.status] = tilStatus
            it[updatedTimestamp] = Instant.now()
        }.map {
            it[HendelserTable.id].value
        }
    }
}