package no.nav.paw.kafkakeygenerator.model

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

enum class IdentitetHendelseStatus {
    VENTER, PROSESSERER, SENDT
}

data class IdentitetHendelseRow(
    val id: Long,
    val arbeidssoekerId: Long? = null,
    val aktorId: String,
    val data: String,
    val status: IdentitetHendelseStatus,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asIdentitetHendelseRow(): IdentitetHendelseRow = IdentitetHendelseRow(
    id = this[IdentitetHendelseTable.id].value,
    arbeidssoekerId = this[IdentitetHendelseTable.arbeidssoekerId],
    aktorId = this[IdentitetHendelseTable.aktorId],
    data = this[IdentitetHendelseTable.data],
    status = this[IdentitetHendelseTable.status],
    insertedTimestamp = this[IdentitetHendelseTable.insertedTimestamp],
    updatedTimestamp = this[IdentitetHendelseTable.updatedTimestamp]
)