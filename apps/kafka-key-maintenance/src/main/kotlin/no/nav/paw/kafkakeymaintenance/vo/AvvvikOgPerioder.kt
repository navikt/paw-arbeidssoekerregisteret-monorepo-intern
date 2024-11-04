package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad

data class AvvvikOgPerioder(
    val avviksMelding: AvviksMelding,
    val perioder: List<PeriodeRad>
)