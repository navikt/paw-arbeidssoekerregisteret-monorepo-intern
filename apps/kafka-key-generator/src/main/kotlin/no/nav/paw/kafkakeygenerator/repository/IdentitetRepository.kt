package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.asIdentitetRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class IdentitetRepository {
    // TODO: Hvordan håndtere soft-slettede?
    fun getByIdentitet(identitet: String): IdentitetRow? = transaction {
        IdentiteterTable.selectAll()
            .where { IdentiteterTable.identitet eq identitet }
            .map { it.asIdentitetRow() }
            .singleOrNull()
    }

    // TODO: Hvordan håndtere soft-slettede?
    fun findByAktorId(aktorId: String): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .orderBy(IdentiteterTable.id)
            .where { IdentiteterTable.aktorId eq aktorId }
            .map { it.asIdentitetRow() }
    }

    fun insert(
        arbeidssoekerId: Long,
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

    fun updateGjeldendeByIdentitet(
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

    fun updateGjeldendeAndStatusByIdentitet(
        identitet: String,
        gjeldende: Boolean,
        status: IdentitetStatus
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.identitet eq identitet)
        }) {
            it[IdentiteterTable.gjeldende] = gjeldende
            it[IdentiteterTable.status] = status
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

    fun updateArbeidssoekerIdAndStatusByAktorId(
        aktorId: String,
        arbeidssoekerId: Long,
        status: IdentitetStatus
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.aktorId eq aktorId)
        }) {
            it[IdentiteterTable.arbeidssoekerId] = arbeidssoekerId
            it[IdentiteterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }
}