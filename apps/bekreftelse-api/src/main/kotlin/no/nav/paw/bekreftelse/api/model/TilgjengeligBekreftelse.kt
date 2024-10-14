package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import java.time.Instant
import java.util.*

data class TilgjengeligBekreftelse(
    val periodeId: UUID,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant,
)

fun BekreftelseRow.asTilgjengeligBekreftelse() = this.data.asTilgjengeligBekreftelse()

private fun BekreftelseTilgjengelig.asTilgjengeligBekreftelse() = TilgjengeligBekreftelse(
    periodeId = this.periodeId,
    bekreftelseId = this.bekreftelseId,
    gjelderFra = this.gjelderFra,
    gjelderTil = this.gjelderTil
)
