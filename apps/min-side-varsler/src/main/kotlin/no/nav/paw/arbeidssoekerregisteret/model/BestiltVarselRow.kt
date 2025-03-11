package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.util.*

data class BestiltVarselRow(
    val bestillingId: UUID,
    val periodeId: UUID,
    val varselId: UUID,
    val identitetsnummer: String,
    val status: BestiltVarselStatus,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null
)

data class InsertBestiltVarselRow(
    val bestillingId: UUID,
    val periodeId: UUID,
    val varselId: UUID,
    val identitetsnummer: String,
)

data class UpdateBestiltVarselRow(
    val varselId: UUID,
    val status: BestiltVarselStatus
)

enum class BestiltVarselStatus {
    VENTER,
    AKTIV,
    SENDT,
    FEILET
}

fun ResultRow.asBestiltVarselRow(): BestiltVarselRow = BestiltVarselRow(
    bestillingId = this[BestilteVarslerTable.bestillingId],
    periodeId = this[BestilteVarslerTable.periodeId],
    varselId = this[BestilteVarslerTable.varselId],
    identitetsnummer = this[BestilteVarslerTable.identitetsnummer],
    status = this[BestilteVarslerTable.status],
    insertedTimestamp = this[BestilteVarslerTable.insertedTimestamp],
    updatedTimestamp = this[BestilteVarslerTable.updatedTimestamp]
)

fun BestiltVarselRow.asInsertVarselRow(): InsertVarselRow = InsertVarselRow(
    periodeId = periodeId,
    varselId = varselId,
    varselKilde = VarselKilde.MANUELL_VARSLING,
    varselType = VarselType.BESKJED,
    varselStatus = VarselStatus.UKJENT,
    hendelseName = VarselEventName.UKJENT,
    hendelseTimestamp = Instant.now()
)
