package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

interface Regler {
    val regler: List<Regel>
    val standardRegel: Regel
    operator fun Opplysning.not(): Condition = HarIkke(this)
}

data class HarIkke( val value: Opplysning) : Condition {
    override fun eval(opplysninger: Iterable<Opplysning>): Boolean = value !in opplysninger
}

interface Condition {
    fun eval(opplysninger: Iterable<Opplysning>): Boolean
}
