package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class ArbeidssoekereSynkRow(
    val version: String,
    val identitetsnummer: String,
    val status: Int,
    val inserted: Instant,
    val updated: Instant? = null
)
