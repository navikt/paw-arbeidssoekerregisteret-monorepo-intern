package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class PeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val startetTimestamp: Instant,
    val avsluttetTimestamp: Instant?,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant?,
    val varsler: List<VarselRow>
)

data class InsertPeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val startetTimestamp: Instant
)

data class UpdatePeriodeRow(
    val periodeId: UUID,
    val identitetsnummer: String,
    val avsluttetTimestamp: Instant?
)

fun ResultRow.asPeriodeRow(varselRows: List<VarselRow>): PeriodeRow = PeriodeRow(
    periodeId = this[PeriodeTable.periodeId],
    identitetsnummer = this[PeriodeTable.identitetsnummer],
    startetTimestamp = this[PeriodeTable.startetTimestamp],
    avsluttetTimestamp = this[PeriodeTable.avsluttetTimestamp],
    insertedTimestamp = this[PeriodeTable.insertedTimestamp],
    updatedTimestamp = this[PeriodeTable.updatedTimestamp],
    varsler = varselRows
)

fun Periode.asInsertPeriodeRow(): InsertPeriodeRow = InsertPeriodeRow(
    periodeId = this.id,
    identitetsnummer = this.identitetsnummer,
    startetTimestamp = this.startet.tidspunkt
)

fun Periode.asUpdatePeriodeRow(): UpdatePeriodeRow = UpdatePeriodeRow(
    periodeId = this.id,
    identitetsnummer = this.identitetsnummer,
    avsluttetTimestamp = this.avsluttet?.tidspunkt
)
