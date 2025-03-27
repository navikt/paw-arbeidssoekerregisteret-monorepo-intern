package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class ArbeidssoekerFileRow(
    val identitetsnummer: String,
    val tidspunktFraKilde: String? = null
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
    val forhaandsgodkjentAvAnsatt: Boolean,
    val feilretting: Feilretting?,
)

enum class PeriodeTilstand {
    STARTET,
    STOPPET;
}

data class Feilretting(
    val feiltype: Feiltype,
    val melding: String,
    val tidspunkt: Instant
)

enum class Feiltype {
    FEIL_TIDSPUNKT,
    FEIL_REGISTRERING;
}
