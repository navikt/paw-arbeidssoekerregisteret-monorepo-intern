package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object BestilteVarslerTable : Table("bestilte_varsler") {
    val bestillingId = javaUUID("bestilling_id").references(BestillingerTable.bestillingId)
    val periodeId = javaUUID("periode_id")
    val varselId = javaUUID("varsel_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val status = enumerationByName<BestiltVarselStatus>("status", 50)
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(varselId)

    fun findAll(paging: Paging = Paging.none()): List<BestiltVarselRow> = transaction {
        selectAll()
            .orderBy(insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestiltVarselRow() }
    }

    fun findByVarselId(varselId: UUID): BestiltVarselRow? = transaction {
        selectAll()
            .where { BestilteVarslerTable.varselId eq varselId }
            .map { it.asBestiltVarselRow() }
            .firstOrNull()
    }

    fun findByBestillingId(
        bestillingId: UUID,
        paging: Paging = Paging.none(),
    ): List<BestiltVarselRow> = transaction {
        selectAll()
            .where { BestilteVarslerTable.bestillingId eq bestillingId }
            .orderBy(insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asBestiltVarselRow() }
    }

    fun findVarselIdByBestillingIdAndStatus(
        bestillingId: UUID,
        status: BestiltVarselStatus,
        paging: Paging = Paging.none(),
    ): List<UUID> = transaction {
        selectAll()
            .where { (BestilteVarslerTable.bestillingId eq bestillingId) and (BestilteVarslerTable.status eq status) }
            .orderBy(insertedTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselId() }
    }

    fun insert(varsel: InsertBestiltVarselRow): Int = transaction {
        insert {
            it[bestillingId] = varsel.bestillingId
            it[periodeId] = varsel.periodeId
            it[varselId] = varsel.varselId
            it[identitetsnummer] = varsel.identitetsnummer
            it[status] = BestiltVarselStatus.VENTER
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(varsel: UpdateBestiltVarselRow): Int = transaction {
        update({
            varselId eq varsel.varselId
        }) {
            it[status] = varsel.status
            it[updatedTimestamp] = Instant.now()
        }
    }

    fun deleteByBestillingId(bestillingId: UUID): Int = transaction {
        deleteWhere { BestilteVarslerTable.bestillingId eq bestillingId }
    }

    fun deleteByVarselId(varselId: UUID): Int = transaction {
        deleteWhere { BestilteVarslerTable.varselId eq varselId }
    }

    fun countByBestillingId(bestillingId: UUID): Long = transaction {
        selectAll()
            .where { BestilteVarslerTable.bestillingId eq bestillingId }
            .count()
    }

    fun countByBestillingIdAndStatus(
        bestillingId: UUID,
        status: BestiltVarselStatus
    ): Long = transaction {
        selectAll()
            .where { (BestilteVarslerTable.bestillingId eq bestillingId) and (BestilteVarslerTable.status eq status) }
            .count()
    }

    fun insertAktivePerioder(bestillingId: UUID) = transaction {
        val status = BestiltVarselStatus.VENTER.name
        exec(
            """
            INSERT INTO bestilte_varsler (
                bestilling_id,
                periode_id,
                varsel_id,
                identitetsnummer,
                status,
                inserted_timestamp
            )
            SELECT '$bestillingId',
                   periode_id,
                   gen_random_uuid(),
                   identitetsnummer,
                   '$status',
                   now()
            FROM perioder
            WHERE avsluttet_timestamp IS NULL
        """.trimIndent()
        )
    }
}