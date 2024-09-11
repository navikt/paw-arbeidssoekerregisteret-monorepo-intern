package no.nav.paw.bekreftelse.api.domain

import java.time.Instant
import java.util.*

data class TilgjengeligBekreftelse(
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant,
)

typealias TilgjengeligBekreftelserResponse = List<TilgjengeligBekreftelse>

fun List<no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig>.toResponse(): TilgjengeligBekreftelserResponse =
    this.map {
        TilgjengeligBekreftelse(
            periodeId = it.periodeId,
            bekreftelseId = it.bekreftelseId,
            gjelderFra = it.gjelderFra,
            gjelderTil = it.gjelderTil
        )
    }

