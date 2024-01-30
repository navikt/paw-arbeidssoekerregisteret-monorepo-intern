package no.nav.paw.arbeidssokerregisteret.evaluering.regler

import no.nav.paw.arbeidssokerregisteret.evaluering.Attributt

data class Regel(
    val beskrivelse: String,
    val attributt: List<Attributt>
)

operator fun String.invoke(vararg attributt: Attributt) = Regel(this, attributt.toList())
