package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

interface Regler {
    val regler: List<Regel>
    val standardRegel: Regel
    operator fun Opplysning.not(): Opplysning = HarIkke(this)
}

data class HarIkke( val value: Opplysning) : Opplysning {
    override val id: String get() = "IKKE_${value.id}"
    override val beskrivelse: String get() = "IKKE_${value.beskrivelse}"
    override fun eval(opplysninger: Iterable<Opplysning>): Boolean = value !in opplysninger
}

interface Condition {
    fun eval(opplysninger: Iterable<Opplysning>): Boolean
}
