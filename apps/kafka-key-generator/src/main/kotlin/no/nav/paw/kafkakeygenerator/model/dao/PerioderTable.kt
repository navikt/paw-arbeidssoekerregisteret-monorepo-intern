package no.nav.paw.kafkakeygenerator.model.dao

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object PerioderTable : LongIdTable("perioder") {
    val periodeId = javaUUID("periode_id")
    val identitet = varchar("identitet", 50)
    val startetTimestamp = timestamp("startet_timestamp")
    val avsluttetTimestamp = timestamp("avsluttet_timestamp").nullable()
    val sourceTimestamp = timestamp("source_timestamp")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun getByPeriodeId(
        periodeId: UUID
    ): PeriodeRow? = selectAll()
        .where { PerioderTable.periodeId eq periodeId }
        .map { it.asPeriodeRow() }
        .singleOrNull()

    fun findByIdentiteter(
        identitetList: List<String>
    ): List<PeriodeRow> = transaction {
        selectAll()
            .where { identitet inList identitetList }
            .map { it.asPeriodeRow() }
    }

    fun insert(
        periodeId: UUID,
        identitet: String,
        startetTimestamp: Instant,
        avsluttetTimestamp: Instant? = null,
        sourceTimestamp: Instant
    ): Int = insert {
        it[PerioderTable.periodeId] = periodeId
        it[PerioderTable.identitet] = identitet
        it[PerioderTable.startetTimestamp] = startetTimestamp
        it[PerioderTable.avsluttetTimestamp] = avsluttetTimestamp
        it[PerioderTable.sourceTimestamp] = sourceTimestamp
        it[insertedTimestamp] = Instant.now()
    }.insertedCount

    fun updateAvsluttetTimestamp(
        periodeId: UUID,
        avsluttetTimestamp: Instant?,
        sourceTimestamp: Instant
    ): Int = update(where = {
        (PerioderTable.periodeId eq periodeId)
    }) {
        it[PerioderTable.avsluttetTimestamp] = avsluttetTimestamp
        it[PerioderTable.sourceTimestamp] = sourceTimestamp
        it[updatedTimestamp] = Instant.now()
    }
}