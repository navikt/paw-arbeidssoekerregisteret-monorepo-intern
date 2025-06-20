package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.HendelserTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

data class HendelseRow(
    val id: Long,
    val arbeidssoekerId: Long? = null,
    val aktorId: String,
    val version: Int,
    val data: String,
    val status: HendelseStatus,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asHendelseRow(): HendelseRow = HendelseRow(
    id = this[HendelserTable.id].value,
    arbeidssoekerId = this[HendelserTable.arbeidssoekerId],
    aktorId = this[HendelserTable.aktorId],
    version = this[HendelserTable.version],
    data = this[HendelserTable.data],
    status = this[HendelserTable.status],
    insertedTimestamp = this[HendelserTable.insertedTimestamp],
    updatedTimestamp = this[HendelserTable.updatedTimestamp]
)