package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo

data class Hwm(
    val partition: Int,
    val offset: Long,
    val topic: String
)