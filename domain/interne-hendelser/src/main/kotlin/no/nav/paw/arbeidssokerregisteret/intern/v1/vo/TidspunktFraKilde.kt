package no.nav.paw.arbeidssokerregisteret.intern.v1.vo

import java.time.Instant

data class TidspunktFraKilde(
    val tidspunkt: Instant,
    val avviksType: AvviksType
)

