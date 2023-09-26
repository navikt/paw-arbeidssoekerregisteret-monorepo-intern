package no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain

import java.time.LocalDateTime
import java.util.*

data class PeriodeDto(
    val id: UUID = UUID.randomUUID(),
    val fraOgMedDato: LocalDateTime = LocalDateTime.now(),
    val tilOgMedDato: LocalDateTime? = null,
    val aktiv: Boolean = tilOgMedDato == null,
)

typealias ArbeidssokerPerioder = List<PeriodeDto>
