package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class ArbeidssoekerFileRow(
    val identitetsnummer: String,
    val tidspunktFraKilde: Instant? = null
)

data class ArbeidssoekerDatabaseRow(
    val version: String,
    val identitetsnummer: String,
    val status: Int,
    val inserted: Instant,
    val updated: Instant? = null
)

data class Arbeidssoeker(
    val version: String,
    val identitetsnummer: String,
    val periodeTilstand: PeriodeTilstand,
    val tidspunktFraKilde: Instant?,
    val forhaandsgodkjentAvAnsatt: Boolean,
)

enum class PeriodeTilstand {
    STARTET,
    STOPPET;
}
