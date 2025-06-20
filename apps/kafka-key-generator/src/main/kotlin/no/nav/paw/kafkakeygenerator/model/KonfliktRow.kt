package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.KonflikterTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

data class KonfliktRow(
    val id: Long,
    val aktorId: String,
    val type: KonfliktType,
    val status: KonfliktStatus,
    val sourceTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
    val identiteter: List<KonfliktIdentitetRow>,
)

fun ResultRow.asKonfliktRow(identiteter: List<KonfliktIdentitetRow>): KonfliktRow = KonfliktRow(
    id = this[KonflikterTable.id].value,
    aktorId = this[KonflikterTable.aktorId],
    type = this[KonflikterTable.type],
    status = this[KonflikterTable.status],
    sourceTimestamp = this[KonflikterTable.sourceTimestamp],
    insertedTimestamp = this[KonflikterTable.insertedTimestamp],
    updatedTimestamp = this[KonflikterTable.updatedTimestamp],
    identiteter = identiteter
)