package no.nav.paw.arbeidssokerregisteret.intern.v1.vo

import java.util.*
data class Situasjon(
    val id: UUID,
    val sendtInn: Metadata,
    val utdanning: Utdanning,
    val helse: Helse,
    val arbeidserfaring: Arbeidserfaring,
    val arbeidsoekersituasjon: Arbeidsoekersituasjon
)
