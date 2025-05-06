package no.nav.paw.identitet.internehendelser.vo

data class Identitet(
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean
)
