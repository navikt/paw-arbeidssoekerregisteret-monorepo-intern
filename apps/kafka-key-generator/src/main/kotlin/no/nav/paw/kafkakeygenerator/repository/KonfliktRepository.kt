package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.database.KonfliktIdentiteterTable
import no.nav.paw.kafkakeygenerator.database.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.KonfliktRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asKonfliktIdentitetRow
import no.nav.paw.kafkakeygenerator.model.asKonfliktRow
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.updateReturning
import java.time.Instant

class KonfliktRepository(
    private val konfliktIdentitetRepository: KonfliktIdentitetRepository
) {

    fun getById(
        id: Long,
    ): KonfliktRow? = transaction {
        KonflikterTable.selectAll()
            .where { KonflikterTable.id eq id }
            .map { it.asKonfliktRowMedIdentiteter() }
            .singleOrNull()
    }

    fun findByAktorId(
        aktorId: String
    ): List<KonfliktRow> = transaction {
        KonflikterTable.selectAll()
            .where { KonflikterTable.aktorId eq aktorId }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByAktorIdAndType(
        aktorId: String,
        type: KonfliktType
    ): List<KonfliktRow> = transaction {
        KonflikterTable.selectAll()
            .where { (KonflikterTable.aktorId eq aktorId) and (KonflikterTable.type eq type) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun insert(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus,
        sourceTimestamp: Instant,
        identiteter: List<Identitet>
    ): Int = transaction {
        val statement = KonflikterTable.insert {
            it[KonflikterTable.aktorId] = aktorId
            it[KonflikterTable.type] = type
            it[KonflikterTable.status] = status
            it[KonflikterTable.sourceTimestamp] = sourceTimestamp
            it[insertedTimestamp] = Instant.now()
        }
        val id = statement[KonflikterTable.id]
        val identiteterInsertedCount = identiteter
            .sumOf { konfliktIdentitetRepository.insert(id.value, it) }
        statement.insertedCount + identiteterInsertedCount
    }

    fun updateStatusByAktorIdAndType(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus
    ): Int = transaction {
        KonflikterTable.update(where = { (KonflikterTable.aktorId eq aktorId) and (KonflikterTable.type eq type) }) {
            it[KonflikterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByTypeAndStatusReturning(
        type: KonfliktType,
        fraStatus: KonfliktStatus,
        tilStatus: KonfliktStatus,
        limit: Int = 1000
    ): List<Long> = transaction {
        val chunkQuery = KonflikterTable.select(KonflikterTable.id)
            .where { (KonflikterTable.type eq type) and (KonflikterTable.status eq fraStatus) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .limit(limit)
        KonflikterTable.updateReturning(
            returning = listOf(KonflikterTable.id),
            where = { KonflikterTable.id inSubQuery chunkQuery }) {
            it[KonflikterTable.status] = tilStatus
            it[updatedTimestamp] = Instant.now()
        }.map {
            it[KonflikterTable.id].value
        }
    }

    private fun ResultRow.asKonfliktRowMedIdentiteter(): KonfliktRow {
        val id = get(KonflikterTable.id)
        val konfliktIdentitetRows = KonfliktIdentiteterTable.selectAll()
            .where { KonfliktIdentiteterTable.konfliktId eq id.value }
            .orderBy(KonfliktIdentiteterTable.id, SortOrder.ASC)
            .map { it.asKonfliktIdentitetRow() }
        return asKonfliktRow(konfliktIdentitetRows)
    }
}