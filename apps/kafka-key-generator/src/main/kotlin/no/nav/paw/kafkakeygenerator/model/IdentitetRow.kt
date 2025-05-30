package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.database.IdentiteterTable
import org.jetbrains.exposed.sql.ResultRow
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