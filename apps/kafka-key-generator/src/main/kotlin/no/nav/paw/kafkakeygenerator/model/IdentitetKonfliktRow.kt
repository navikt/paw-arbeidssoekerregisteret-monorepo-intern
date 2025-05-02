package no.nav.paw.kafkakeygenerator.model

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

enum class IdentitetKonfliktStatus {
    VENTER, PROSESSERER, FULFOERT
}

data class IdentitetKonfliktRow(
    val id: Long,
    val aktorId: String,
    val status: IdentitetKonfliktStatus,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asIdentitetKonfliktRow(): IdentitetKonfliktRow = IdentitetKonfliktRow(
    id = this[IdentitetKonflikterTable.id].value,
    aktorId = this[IdentitetKonflikterTable.aktorId],
    status = this[IdentitetKonflikterTable.status],
    insertedTimestamp = this[IdentitetKonflikterTable.insertedTimestamp],
    updatedTimestamp = this[IdentitetKonflikterTable.updatedTimestamp]
)