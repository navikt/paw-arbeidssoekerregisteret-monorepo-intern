package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.database.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.IdentitetRow
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitetRow
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class IdentitetRepository {
    // OBS! Har med soft-slettede
    fun getByIdentitet(identitet: String): IdentitetRow? = transaction {
        IdentiteterTable.selectAll()
            .where { IdentiteterTable.identitet eq identitet }
            .map { it.asIdentitetRow() }
            .singleOrNull()
    }

    // OBS! Har med soft-slettede
    fun findByIdentitet(identitet: String): List<IdentitetRow> = transaction {
        val i1 = IdentiteterTable.alias("i1")
        val i2 = IdentiteterTable.alias("i2")
        i1.join(i2, JoinType.INNER, i1[IdentiteterTable.arbeidssoekerId], i2[IdentiteterTable.arbeidssoekerId])
            .select(i2.columns)
            .where { i1[IdentiteterTable.identitet] eq identitet }
            .map { i2 to it }
            .map { it.asIdentitetRow() }
    }

    // OBS! Har med soft-slettede
    fun findByAktorId(aktorId: String): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .orderBy(IdentiteterTable.id)
            .where { IdentiteterTable.aktorId eq aktorId }
            .map { it.asIdentitetRow() }
    }

    // OBS! Har med soft-slettede
    fun findByArbeidssoekerId(arbeidssoekerId: Long): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .orderBy(IdentiteterTable.id)
            .where { IdentiteterTable.arbeidssoekerId eq arbeidssoekerId }
            .map { it.asIdentitetRow() }
    }

    // OBS! Har med soft-slettede
    fun findByAktorIdOrIdentiteter(
        aktorId: String,
        identiteter: Iterable<String>
    ): List<IdentitetRow> = transaction {
        IdentiteterTable.selectAll()
            .orderBy(IdentiteterTable.id)
            .where { (IdentiteterTable.aktorId eq aktorId) or (IdentiteterTable.identitet inList identiteter) }
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

    fun updateByIdentitet(
        identitet: String,
        aktorId: String,
        gjeldende: Boolean,
        status: IdentitetStatus,
        sourceTimestamp: Instant
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.identitet eq identitet) and
                    ((IdentiteterTable.aktorId neq aktorId) or
                            (IdentiteterTable.gjeldende neq gjeldende) or
                            (IdentiteterTable.status neq status))
        }) {
            it[IdentiteterTable.aktorId] = aktorId
            it[IdentiteterTable.gjeldende] = gjeldende
            it[IdentiteterTable.status] = status
            it[IdentiteterTable.sourceTimestamp] = sourceTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateByIdentitet(
        identitet: String,
        aktorId: String,
        arbeidssoekerId: Long,
        gjeldende: Boolean,
        status: IdentitetStatus,
        sourceTimestamp: Instant
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.identitet eq identitet) and
                    ((IdentiteterTable.aktorId neq aktorId) or
                            (IdentiteterTable.arbeidssoekerId neq arbeidssoekerId) or
                            (IdentiteterTable.gjeldende neq gjeldende) or
                            (IdentiteterTable.status neq status))
        }) {
            it[IdentiteterTable.aktorId] = aktorId
            it[IdentiteterTable.arbeidssoekerId] = arbeidssoekerId
            it[IdentiteterTable.gjeldende] = gjeldende
            it[IdentiteterTable.status] = status
            it[IdentiteterTable.sourceTimestamp] = sourceTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateGjeldendeAndStatusByAktorId(
        aktorId: String,
        gjeldende: Boolean,
        status: IdentitetStatus
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.aktorId eq aktorId) and
                    ((IdentiteterTable.gjeldende neq gjeldende) or
                            (IdentiteterTable.status neq status))
        }) {
            it[IdentiteterTable.gjeldende] = gjeldende
            it[IdentiteterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun updateStatusByNotSlettetAndAktorIdList(
        status: IdentitetStatus,
        aktorIdList: Iterable<String>
    ): Int = transaction {
        IdentiteterTable.update(where = {
            (IdentiteterTable.status neq IdentitetStatus.SLETTET) and
                    (IdentiteterTable.aktorId inList aktorIdList)
        }) {
            it[IdentiteterTable.status] = status
            it[updatedTimestamp] = Instant.now()
        }
    }
}