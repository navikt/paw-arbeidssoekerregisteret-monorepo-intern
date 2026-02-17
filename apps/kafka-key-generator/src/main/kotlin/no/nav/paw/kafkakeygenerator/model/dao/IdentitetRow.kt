package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant

data class IdentitetRow(
    val id: Long,
    val arbeidssoekerId: Long,
    val aktorId: String,
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean,
    val status: IdentitetStatus,
    val sourceTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asIdentitetRow(): IdentitetRow = IdentitetRow(
    id = this[IdentiteterTable.id].value,
    arbeidssoekerId = this[IdentiteterTable.arbeidssoekerId],
    aktorId = this[IdentiteterTable.aktorId],
    identitet = this[IdentiteterTable.identitet],
    type = this[IdentiteterTable.type],
    gjeldende = this[IdentiteterTable.gjeldende],
    status = this[IdentiteterTable.status],
    sourceTimestamp = this[IdentiteterTable.sourceTimestamp],
    insertedTimestamp = this[IdentiteterTable.insertedTimestamp],
    updatedTimestamp = this[IdentiteterTable.updatedTimestamp]
)

fun Pair<Alias<IdentiteterTable>, ResultRow>.asIdentitetRow(): IdentitetRow {
    val (alias, row) = this
    return IdentitetRow(
        id = row[alias[IdentiteterTable.id]].value,
        arbeidssoekerId = row[alias[IdentiteterTable.arbeidssoekerId]],
        aktorId = row[alias[IdentiteterTable.aktorId]],
        identitet = row[alias[IdentiteterTable.identitet]],
        type = row[alias[IdentiteterTable.type]],
        gjeldende = row[alias[IdentiteterTable.gjeldende]],
        status = row[alias[IdentiteterTable.status]],
        sourceTimestamp = row[alias[IdentiteterTable.sourceTimestamp]],
        insertedTimestamp = row[alias[IdentiteterTable.insertedTimestamp]],
        updatedTimestamp = row[alias[IdentiteterTable.updatedTimestamp]]
    )
}