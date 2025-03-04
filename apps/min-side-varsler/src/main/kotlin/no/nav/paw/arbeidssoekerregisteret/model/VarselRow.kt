package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class VarselRow(
    val periodeId: UUID,
    val varselId: UUID,
    val varselKilde: VarselKilde,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant?
)

data class InsertVarselRow(
    val periodeId: UUID,
    val varselId: UUID,
    val varselKilde: VarselKilde,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

data class UpdateVarselRow(
    val varselId: UUID,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

fun ResultRow.asVarselRow(): VarselRow = VarselRow(
    periodeId = this[VarselTable.periodeId],
    varselId = this[VarselTable.varselId],
    varselKilde = this[VarselTable.varselKilde],
    varselType = this[VarselTable.varselType],
    varselStatus = this[VarselTable.varselStatus],
    hendelseName = this[VarselTable.hendelseNavn],
    hendelseTimestamp = this[VarselTable.hendelseTimestamp],
    insertedTimestamp = this[VarselTable.insertedTimestamp],
    updatedTimestamp = this[VarselTable.updatedTimestamp]
)

fun BekreftelseTilgjengelig.asInsertVarselRow(
    varselKilde: VarselKilde,
    varselType: VarselType,
) = InsertVarselRow(
    periodeId = this.periodeId,
    varselId = this.bekreftelseId,
    varselKilde = varselKilde,
    varselType = varselType,
    varselStatus = VarselStatus.UKJENT,
    hendelseName = VarselEventName.UKJENT,
    hendelseTimestamp = this.hendelseTidspunkt
)

fun VarselHendelse.asUpdateVarselRow() = UpdateVarselRow(
    varselId = UUID.fromString(this.varselId),
    varselStatus = this.status ?: VarselStatus.UKJENT,
    hendelseName = this.eventName,
    hendelseTimestamp = this.tidspunkt
)
