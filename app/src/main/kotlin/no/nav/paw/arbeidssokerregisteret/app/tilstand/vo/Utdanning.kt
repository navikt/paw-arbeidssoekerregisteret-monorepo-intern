package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

data class Utdanning(
    val utdanningsnivaa: Utdanningsnivaa,
    val bestaatt: JaNeiVetIkke,
    val godkjent: JaNeiVetIkke,
)