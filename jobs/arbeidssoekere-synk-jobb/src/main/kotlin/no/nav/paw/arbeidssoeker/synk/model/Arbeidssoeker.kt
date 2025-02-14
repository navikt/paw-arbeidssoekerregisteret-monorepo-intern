package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class Arbeidssoeker(
    val identitetsnummer: String,
    val originalStartTidspunkt: Instant
)

data class VersjonertArbeidssoeker(
    val version: String,
    val identitetsnummer: String,
    val originalStartTidspunkt: Instant,
    val forhaandsgodkjentAvAnsatt: Boolean = false,
)
