package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object PerioderTable : Table("perioder") {
    val periodeId = javaUUID("periode_id")
    val identitetsnummer = varchar("identitetsnummer", 20)
    val startetTimestamp = timestamp("startet_timestamp")
    val avsluttetTimestamp = timestamp("avsluttet_timestamp").nullable()
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()
    override val primaryKey: PrimaryKey = PrimaryKey(periodeId)

    fun findAll(paging: Paging = Paging.none()): List<PeriodeRow> = transaction {
        selectAll()
            .orderBy(startetTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asPeriodeRow() }
    }

    fun findByPeriodeId(periodeId: UUID): PeriodeRow? = transaction {
        selectAll()
            .where { PerioderTable.periodeId eq periodeId }
            .map { it.asPeriodeRow() }
            .singleOrNull()
    }

    fun insert(periode: InsertPeriodeRow): Int = transaction {
        insert {
            it[periodeId] = periode.periodeId
            it[identitetsnummer] = periode.identitetsnummer
            it[startetTimestamp] = periode.startetTimestamp
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun update(periode: UpdatePeriodeRow): Int = transaction {
        PerioderTable.update({
            periodeId eq periode.periodeId
        }) {
            it[identitetsnummer] = periode.identitetsnummer
            it[avsluttetTimestamp] = periode.avsluttetTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}