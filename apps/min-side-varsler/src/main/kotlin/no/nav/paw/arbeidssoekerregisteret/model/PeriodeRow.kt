package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class PeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val startetTimestamp: Instant,
    val avsluttetTimestamp: Instant? = null,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

data class InsertPeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val startetTimestamp: Instant,
    val avsluttetTimestamp: Instant? = null
)

data class UpdatePeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val avsluttetTimestamp: Instant? = null
)

fun ResultRow.asPeriodeRow(): PeriodeRow = PeriodeRow(
    periodeId = this[PerioderTable.periodeId],
    identitetsnummer = this[PerioderTable.identitetsnummer],
    startetTimestamp = this[PerioderTable.startetTimestamp],
    avsluttetTimestamp = this[PerioderTable.avsluttetTimestamp],
    insertedTimestamp = this[PerioderTable.insertedTimestamp],
    updatedTimestamp = this[PerioderTable.updatedTimestamp],
)

fun Periode.asInsertPeriodeRow(): InsertPeriodeRow = InsertPeriodeRow(
    periodeId = this.id,
    identitetsnummer = this.identitetsnummer,
    startetTimestamp = this.startet.tidspunkt,
    avsluttetTimestamp = this.avsluttet?.tidspunkt
)

fun Periode.asUpdatePeriodeRow(): UpdatePeriodeRow = UpdatePeriodeRow(
    periodeId = this.id,
    identitetsnummer = this.identitetsnummer,
    avsluttetTimestamp = this.avsluttet?.tidspunkt
)
