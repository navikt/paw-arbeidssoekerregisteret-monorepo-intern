package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.logging.logger.buildNamedLogger
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.sql.SQLException
import java.time.Instant

private val logger = buildNamedLogger("database.konflikter")

object KonflikterTable : LongIdTable("konflikter") {
    val aktorId = varchar("aktor_id", 50)
    val type = enumerationByName<KonfliktType>("type", 50)
    val status = enumerationByName<KonfliktStatus>("status", 50)
    val sourceTimestamp = timestamp("source_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun findByIdList(
        idList: Collection<Long>,
    ): List<KonfliktRow> = transaction {
        selectAll()
            .where { KonflikterTable.id inList idList }
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByAktorId(
        aktorId: String
    ): List<KonfliktRow> = transaction {
        selectAll()
            .where { KonflikterTable.aktorId eq aktorId }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByAktorIdAndType(
        aktorId: String,
        type: KonfliktType
    ): List<KonfliktRow> = transaction {
        selectAll()
            .where { (KonflikterTable.aktorId eq aktorId) and (KonflikterTable.type eq type) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByAktorIdAndStatus(
        aktorId: String,
        status: KonfliktStatus
    ): List<KonfliktRow> = transaction {
        selectAll()
            .where { (KonflikterTable.aktorId eq aktorId) and (KonflikterTable.status eq status) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByTypeAndStatus(
        type: KonfliktType,
        status: KonfliktStatus,
        rowCount: Int
    ): List<KonfliktRow> = transaction {
        selectAll()
            .where { (KonflikterTable.type eq type) and (KonflikterTable.status eq status) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .limit(rowCount)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun findByIdentitetAndStatus(
        identitet: String,
        status: KonfliktStatus
    ): List<KonfliktRow> = transaction {
        join(
            KonfliktIdentiteterTable,
            JoinType.INNER,
            KonflikterTable.id,
            KonfliktIdentiteterTable.konfliktId
        ).select(columns)
            .where { (KonfliktIdentiteterTable.identitet eq identitet) and (KonflikterTable.status eq status) }
            .orderBy(KonflikterTable.id, SortOrder.ASC)
            .map { it.asKonfliktRowMedIdentiteter() }
    }

    fun insert(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus,
        sourceTimestamp: Instant,
        identiteter: List<Identitet>
    ): Int = runCatching {
        transaction {
            val statement = insert {
                it[KonflikterTable.aktorId] = aktorId
                it[KonflikterTable.type] = type
                it[KonflikterTable.status] = status
                it[KonflikterTable.sourceTimestamp] = sourceTimestamp
                it[insertedTimestamp] = Instant.now()
            }
            val id = statement[KonflikterTable.id]
            val identiteterInsertedCount = identiteter
                .sumOf { KonfliktIdentiteterTable.insert(id.value, it) }
            statement.insertedCount + identiteterInsertedCount
        }
    }.getOrElse { throwable ->
        logger.trace("Feil ved insert av konflikt-identitet", throwable)
        throw SQLException("Feil ved insert av konflikt-identitet")
    }

    fun updateStatusByAktorIdAndType(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus
    ): Int = transaction {
        update(where = { (KonflikterTable.aktorId eq aktorId) and (KonflikterTable.type eq type) }) {
            it[KonflikterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByIdListReturning(
        idList: Collection<Long>,
        fraStatus: KonfliktStatus,
        tilStatus: KonfliktStatus,
    ): List<Long> = transaction {
        updateReturning(
            returning = listOf(KonflikterTable.id),
            where = { (KonflikterTable.id inList idList) and (status eq fraStatus) }) {
            it[KonflikterTable.status] = tilStatus
            it[updatedTimestamp] = Instant.now()
        }.map {
            it[KonflikterTable.id].value
        }
    }

    private fun ResultRow.asKonfliktRowMedIdentiteter(): KonfliktRow {
        val id = get(id)
        val konfliktIdentitetRows = KonfliktIdentiteterTable.selectAll()
            .where { KonfliktIdentiteterTable.konfliktId eq id.value }
            .orderBy(KonfliktIdentiteterTable.id, SortOrder.ASC)
            .map { it.asKonfliktIdentitetRow() }
        return asKonfliktRow(konfliktIdentitetRows)
    }
}