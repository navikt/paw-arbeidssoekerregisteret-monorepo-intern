package no.nav.paw.bekreftelse.api.repository

import no.nav.paw.bekreftelse.api.model.BekreftelseRow
import no.nav.paw.bekreftelse.api.model.BekreftelserTable
import no.nav.paw.bekreftelse.api.model.asBekreftelseRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.*

class BekreftelseRepository {

    fun getByBekreftelseId(bekreftelseId: UUID): BekreftelseRow? {
        return BekreftelserTable.selectAll()
            .where { BekreftelserTable.bekreftelseId eq bekreftelseId }
            .singleOrNull()?.asBekreftelseRow()
    }

    fun findByArbeidssoekerId(arbeidssokerId: Long): List<BekreftelseRow> {
        return BekreftelserTable.selectAll()
            .where { BekreftelserTable.arbeidssoekerId eq arbeidssokerId }
            .map { it.asBekreftelseRow() }
    }

    fun insert(row: BekreftelseRow): Int {
        val result = BekreftelserTable.insert {
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
        return BekreftelserTable.update(where = {
            (BekreftelserTable.arbeidssoekerId eq row.arbeidssoekerId) and
                    (BekreftelserTable.periodeId eq row.periodeId) and
                    (BekreftelserTable.bekreftelseId eq row.bekreftelseId)
        }) {
            it[version] = row.version
            it[partition] = row.partition
            it[offset] = row.offset
            it[recordKey] = row.recordKey
            it[data] = row.data
        }
    }

    fun deleteByBekreftelseId(bekreftelseId: UUID): Int {
        return BekreftelserTable.deleteWhere { BekreftelserTable.bekreftelseId eq bekreftelseId }
    }

    fun deleteByPeriodeId(periodeId: UUID): Int {
        return BekreftelserTable.deleteWhere { BekreftelserTable.periodeId eq periodeId }
    }
}