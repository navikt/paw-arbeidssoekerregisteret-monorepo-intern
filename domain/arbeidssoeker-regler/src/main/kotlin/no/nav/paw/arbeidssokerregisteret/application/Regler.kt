package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

interface Regler {
    val regler: List<Regel>
    val standardRegel: Regel
    operator fun Opplysning.not(): Opplysning = HarIkke(this).value
}

interface Not<A> {
    val value: A
}

data class HarIkke(override val value: Opplysning) : Not<Opplysning>
