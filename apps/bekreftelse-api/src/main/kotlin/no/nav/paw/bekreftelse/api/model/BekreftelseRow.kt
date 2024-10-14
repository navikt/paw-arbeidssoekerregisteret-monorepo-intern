package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

data class BekreftelseRow(
    val version: Int,
    val partition: Int,
    val offset: Long,
    val recordKey: Long,
    val arbeidssoekerId: Long,
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val data: BekreftelseTilgjengelig
) {
    fun harSammeOffset(row: BekreftelseRow): Boolean {
        return version == row.version && partition == row.partition && offset == row.offset
    }
}

fun ResultRow.asBekreftelseRow() = BekreftelseRow(
    version = this[BekreftelserTable.version],
    partition = this[BekreftelserTable.partition],
    offset = this[BekreftelserTable.offset],
    recordKey = this[BekreftelserTable.recordKey],
    arbeidssoekerId = this[BekreftelserTable.arbeidssoekerId],
    periodeId = this[BekreftelserTable.periodeId],
    bekreftelseId = this[BekreftelserTable.bekreftelseId],
    data = this[BekreftelserTable.data]
)

fun BekreftelseTilgjengelig.asBekreftelseRow(
    version: Int,
    partition: Int,
    offset: Long,
    recordKey: Long,
) = BekreftelseRow(
    version = version,
    partition = partition,
    offset = offset,
    recordKey = recordKey,
    arbeidssoekerId = this.arbeidssoekerId,
    periodeId = this.periodeId,
    bekreftelseId = this.bekreftelseId,
    data = this
)
