package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class VarselRow(
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant?
)

data class InsertVarselRow(
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

data class UpdateVarselRow(
    val bekreftelseId: UUID,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

fun ResultRow.asVarselRow(): VarselRow = VarselRow(
    periodeId = this[VarselTable.periodeId],
    bekreftelseId = this[VarselTable.bekreftelseId],
    varselType = this[VarselTable.varselType],
    varselStatus = this[VarselTable.varselStatus],
    hendelseName = this[VarselTable.hendelseNavn],
    hendelseTimestamp = this[VarselTable.hendelseTimestamp],
    insertedTimestamp = this[VarselTable.insertedTimestamp],
    updatedTimestamp = this[VarselTable.updatedTimestamp]
)

fun BekreftelseTilgjengelig.asInsertVarselRow() = InsertVarselRow(
    periodeId = this.periodeId,
    bekreftelseId = this.bekreftelseId,
    varselType = VarselType.OPPGAVE,
    varselStatus = VarselStatus.UKJENT,
    hendelseName = VarselEventName.UKJENT,
    hendelseTimestamp = this.hendelseTidspunkt
)

fun VarselHendelse.asUpdateVarselRow() = UpdateVarselRow(
    bekreftelseId = UUID.fromString(this.varselId),
    varselStatus = this.status ?: VarselStatus.UKJENT,
    hendelseName = this.eventName,
    hendelseTimestamp = this.tidspunkt
)
