package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.PerioderTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class PeriodeRow(
    val periodeId: UUID,
    val identitet: String,
    val startetTimestamp: Instant,
    val avsluttetTimestamp: Instant? = null,
    val sourceTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asPeriodeRow(): PeriodeRow = PeriodeRow(
    periodeId = this[PerioderTable.periodeId],
    identitet = this[PerioderTable.identitet],
    startetTimestamp = this[PerioderTable.startetTimestamp],
    avsluttetTimestamp = this[PerioderTable.avsluttetTimestamp],
    sourceTimestamp = this[PerioderTable.sourceTimestamp],
    insertedTimestamp = this[PerioderTable.insertedTimestamp],
    updatedTimestamp = this[PerioderTable.updatedTimestamp]
)