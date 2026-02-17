package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant
import java.util.*

data class BestillingRow(
    val bestillingId: UUID,
    val bestiller: String,
    val status: BestillingStatus,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null
)

data class InsertBestillingRow(
    val bestillingId: UUID,
    val bestiller: String
)

data class UpdateBestillingRow(
    val bestillingId: UUID,
    val status: BestillingStatus
)

enum class BestillingStatus {
    VENTER,
    BEKREFTET,
    AKTIV,
    SENDT,
    IGNORERT,
    FEILET
}

fun ResultRow.asBestillingRow(): BestillingRow = BestillingRow(
    bestillingId = this[BestillingerTable.bestillingId],
    bestiller = this[BestillingerTable.bestiller],
    status = this[BestillingerTable.status],
    insertedTimestamp = this[BestillingerTable.insertedTimestamp],
    updatedTimestamp = this[BestillingerTable.updatedTimestamp]
)
