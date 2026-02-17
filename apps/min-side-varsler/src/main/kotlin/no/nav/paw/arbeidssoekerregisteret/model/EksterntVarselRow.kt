package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant
import java.util.*

data class EksterntVarselRow(
    val varselId: UUID,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null
)

data class InsertEksterntVarselRow(
    val varselId: UUID,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

data class UpdateEksterntVarselRow(
    val varselId: UUID,
    val varselType: VarselType,
    val varselStatus: VarselStatus,
    val hendelseName: VarselEventName,
    val hendelseTimestamp: Instant
)

fun ResultRow.asEksterntVarselRow(): EksterntVarselRow = EksterntVarselRow(
    varselId = this[EksterneVarslerTable.varselId],
    varselType = this[EksterneVarslerTable.varselType],
    varselStatus = this[EksterneVarslerTable.varselStatus],
    hendelseName = this[EksterneVarslerTable.hendelseNavn],
    hendelseTimestamp = this[EksterneVarslerTable.hendelseTimestamp],
    insertedTimestamp = this[EksterneVarslerTable.insertedTimestamp],
    updatedTimestamp = this[EksterneVarslerTable.updatedTimestamp]
)

fun ResultRow.asEksterntVarselRowOrNull(): EksterntVarselRow? {
    return if (this.getOrNull(EksterneVarslerTable.varselId) == null) {
        null
    } else {
        EksterntVarselRow(
            varselId = this[EksterneVarslerTable.varselId],
            varselType = this[EksterneVarslerTable.varselType],
            varselStatus = this[EksterneVarslerTable.varselStatus],
            hendelseName = this[EksterneVarslerTable.hendelseNavn],
            hendelseTimestamp = this[EksterneVarslerTable.hendelseTimestamp],
            insertedTimestamp = this[EksterneVarslerTable.insertedTimestamp],
            updatedTimestamp = this[EksterneVarslerTable.updatedTimestamp]
        )
    }
}


fun VarselHendelse.asInsertEksterntVarselRow() = InsertEksterntVarselRow(
    varselId = UUID.fromString(this.varselId),
    varselType = this.varseltype,
    varselStatus = this.status ?: VarselStatus.UKJENT,
    hendelseName = this.eventName,
    hendelseTimestamp = this.tidspunkt
)

fun VarselHendelse.asUpdateEksterntVarselRow() = UpdateEksterntVarselRow(
    varselId = UUID.fromString(this.varselId),
    varselType = this.varseltype,
    varselStatus = this.status ?: VarselStatus.UKJENT,
    hendelseName = this.eventName,
    hendelseTimestamp = this.tidspunkt
)
