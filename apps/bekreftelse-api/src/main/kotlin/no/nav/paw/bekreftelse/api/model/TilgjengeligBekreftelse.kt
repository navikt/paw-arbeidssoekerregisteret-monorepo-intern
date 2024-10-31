package no.nav.paw.bekreftelse.api.model

import java.time.Instant
import java.util.*

data class TilgjengeligBekreftelse(
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant,
)
