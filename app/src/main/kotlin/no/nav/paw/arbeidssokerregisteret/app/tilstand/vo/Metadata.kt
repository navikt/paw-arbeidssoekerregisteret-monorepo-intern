package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import java.time.Instant

data class Metadata(
    val tidspunkt: Instant,
    val utfoertAv: Bruker,
    val kilde: String,
    val aarsak: String
)