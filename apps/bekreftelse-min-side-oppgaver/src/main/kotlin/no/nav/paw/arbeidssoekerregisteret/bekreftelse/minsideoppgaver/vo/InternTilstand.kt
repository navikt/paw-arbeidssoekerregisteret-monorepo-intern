package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo

import java.util.*

data class InternTilstand(
    val periodeId: UUID,
    val ident: String,
    val bekreftelser: List<UUID>
)