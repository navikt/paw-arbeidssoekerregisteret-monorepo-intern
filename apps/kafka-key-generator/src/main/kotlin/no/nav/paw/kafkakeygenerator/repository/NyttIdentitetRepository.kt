package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class NyttIdentitetRepository {
    fun getByIdentitet(identitet: String): IdentitetRow? = transaction {
        IdentiteterTable.selectAll()
            .where { IdentiteterTable.identitet eq identitet }
            .map { it.asIdentitetRow() }
            .singleOrNull()
    }

    fun findByAktorId(aktorId: String): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .where { IdentiteterTable.aktorId eq aktorId }
            .map { it.asIdentitetRow() }
    }

    fun findByIdentiteter(identitetList: List<String>): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .where { IdentiteterTable.identitet inList identitetList }
            .map { it.asIdentitetRow() }
    }

    fun insert(
        arbeidssoekerId: Long? = null,
        aktorId: String,
        identitet: String,
        type: IdentitetType,
        gjeldende: Boolean,
        status: IdentitetStatus,
        sourceTimestamp: Instant
    ): Int = transaction {
        IdentiteterTable.insert {
            it[IdentiteterTable.arbeidssoekerId] = arbeidssoekerId
            it[IdentiteterTable.aktorId] = aktorId
            it[IdentiteterTable.identitet] = identitet
            it[IdentiteterTable.type] = type
            it[IdentiteterTable.gjeldende] = gjeldende
            it[IdentiteterTable.status] = status
            it[IdentiteterTable.sourceTimestamp] = sourceTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateArbeidssoekerId(
        identitet: String,
        arbeidssoekerId: Long
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.identitet eq identitet)
        }) {
            it[IdentiteterTable.arbeidssoekerId] = arbeidssoekerId
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateGjeldende(
        identitet: String,
        gjeldende: Boolean
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.identitet eq identitet)
        }) {
            it[IdentiteterTable.gjeldende] = gjeldende
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByAktorId(
        aktorId: String,
        status: IdentitetStatus
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.aktorId eq aktorId)
        }) {
            it[IdentiteterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateByAktorId(
        aktorId: String,
        gjeldende: Boolean
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.aktorId eq aktorId)
        }) {
            it[IdentiteterTable.gjeldende] = gjeldende
            it[updatedTimestamp] = Instant.now()
        }
    }
}