package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.api.utils.JsonSerde
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.json.jsonb
import java.util.*

object BekreftelserTable : Table("bekreftelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val periodeId = javaUUID("periode_id")
    val bekreftelseId = javaUUID("bekreftelse_id")
    val data = jsonb("data", JsonSerde::serialize, JsonSerde::deserialize)
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)

    fun getByBekreftelseId(bekreftelseId: UUID): BekreftelseRow? {
        return selectAll()
            .where { BekreftelserTable.bekreftelseId eq bekreftelseId }
            .singleOrNull()?.asBekreftelseRow()
    }

    fun findByArbeidssoekerId(arbeidssoekerId: Long): List<BekreftelseRow> {
        return selectAll()
            .where { BekreftelserTable.arbeidssoekerId eq arbeidssoekerId }
            .map { it.asBekreftelseRow() }
    }

    fun insert(row: BekreftelseRow): Int {
        val result = insert {
            it[version] = row.version
            it[partition] = row.partition
            it[offset] = row.offset
            it[recordKey] = row.recordKey
            it[arbeidssoekerId] = row.arbeidssoekerId
            it[periodeId] = row.periodeId
            it[bekreftelseId] = row.bekreftelseId
            it[data] = row.data
        }
        return result.insertedCount
    }

    fun update(row: BekreftelseRow): Int {
        return update(where = {
            (arbeidssoekerId eq row.arbeidssoekerId) and
                    (periodeId eq row.periodeId) and
                    (bekreftelseId eq row.bekreftelseId)
        }) {
            it[version] = row.version
            it[partition] = row.partition
            it[offset] = row.offset
            it[recordKey] = row.recordKey
            it[data] = row.data
        }
    }

    fun deleteByBekreftelseId(bekreftelseId: UUID): Int {
        return deleteWhere { BekreftelserTable.bekreftelseId eq bekreftelseId }
    }

    fun deleteByPeriodeId(periodeId: UUID): Int {
        return deleteWhere { BekreftelserTable.periodeId eq periodeId }
    }
}