package no.nav.paw.arbeidssoeker.synk.model

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

data class ArbeidssoekereSynkRow(
    val version: String,
    val identitetsnummer: String,
    val status: Int,
    val inserted: Instant,
    val updated: Instant? = null
)

fun ResultRow.asArbeidssoekereSynkRow(): ArbeidssoekereSynkRow =
    ArbeidssoekereSynkRow(
        version = this[ArbeidssoekereSynkTable.version],
        identitetsnummer = this[ArbeidssoekereSynkTable.identitetsnummer],
        status = this[ArbeidssoekereSynkTable.status],
        inserted = this[ArbeidssoekereSynkTable.inserted],
        updated = this[ArbeidssoekereSynkTable.updated]
    )
