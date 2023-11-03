package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import java.util.*

data class Situasjon(
    val id: UUID,
    val sendId: Metadata,
    val utdanning: Utdanning,
    val helse: Helse,
    val arbeidserfaring: Arbeidserfaring,
)
