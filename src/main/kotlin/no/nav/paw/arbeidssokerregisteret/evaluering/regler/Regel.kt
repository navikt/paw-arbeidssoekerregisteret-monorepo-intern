package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.evaluering.Attributter

data class Regel(
    val beskrivelse: String,
    val attributter: List<Attributter>
)

operator fun String.invoke(vararg attributter: Attributter) = Regel(this, attributter.toList())
