package no.nav.paw.bekreftelse.internehendelser.vo

data class Bruker(
    val type: BrukerType,
    val id: String,
    val sikkerhetsnivaa: String?
)
