package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta

data class Regel(
    /**
     * Beskrivelse av regelen
     */
    val beskrivelse: String,
    /**
     * Fakta som må være tilstede for at regelen skal være sann
     */
    val fakta: List<Fakta>
)

operator fun String.invoke(vararg fakta: Fakta) = Regel(this, fakta.toList())

fun Regel.evaluer(samletFakta: Iterable<Fakta>): Boolean = fakta.all { samletFakta.contains(it) }
