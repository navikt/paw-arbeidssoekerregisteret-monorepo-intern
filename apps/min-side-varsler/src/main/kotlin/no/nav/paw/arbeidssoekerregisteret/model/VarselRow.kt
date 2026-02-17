package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant
import java.util.*

data class VarselRow(
    val periodeId: UUID,
    val bekreftelseId: UUID? = null,
    val varselId: UUID,
    val varselKilde: VarselKilde,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
    val eksterntVarsel: EksterntVarselRow? = null
)

data class InsertVarselRow(
    val periodeId: UUID,
    val bekreftelseId: UUID? = null,
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
    periodeId = this[VarslerTable.periodeId],
    bekreftelseId = this[VarslerTable.bekreftelseId],
    varselId = this[VarslerTable.varselId],
    varselKilde = this[VarslerTable.varselKilde],
    varselType = this[VarslerTable.varselType],
    varselStatus = this[VarslerTable.varselStatus],
    hendelseName = this[VarslerTable.hendelseNavn],
    hendelseTimestamp = this[VarslerTable.hendelseTimestamp],
    insertedTimestamp = this[VarslerTable.insertedTimestamp],
    updatedTimestamp = this[VarslerTable.updatedTimestamp],
    eksterntVarsel = this.asEksterntVarselRowOrNull()
)

fun Periode.asInsertVarselRow(varselId: UUID = UUID.randomUUID()) = InsertVarselRow(
    periodeId = this.id,
    varselId = varselId,
    varselKilde = VarselKilde.PERIODE_AVSLUTTET,
    varselType = VarselType.BESKJED,
    varselStatus = VarselStatus.UKJENT,
    hendelseName = VarselEventName.UKJENT,
    hendelseTimestamp = this.avsluttet?.tidspunkt ?: Instant.now()
)

fun BekreftelseTilgjengelig.asInsertVarselRow(varselId: UUID = UUID.randomUUID()) = InsertVarselRow(
    periodeId = this.periodeId,
    bekreftelseId = this.bekreftelseId,
    varselId = varselId,
    varselKilde = VarselKilde.BEKREFTELSE_TILGJENGELIG,
    varselType = VarselType.OPPGAVE,
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
