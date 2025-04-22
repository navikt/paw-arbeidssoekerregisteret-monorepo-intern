package no.nav.paw.identitet.internehendelser.vo

data class Identitet(
    val arbeidssoekerId: Long? = null,
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean
)
