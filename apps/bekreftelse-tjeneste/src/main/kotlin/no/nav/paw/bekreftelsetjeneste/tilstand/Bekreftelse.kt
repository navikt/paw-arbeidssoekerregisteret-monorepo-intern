package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.Instant
import java.util.*

@JvmRecord
data class Bekreftelse(
    val tilstandsLogg: BekreftelseTilstandsLogg,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)

inline fun <reified T: BekreftelseTilstandStatus> Bekreftelse.tilstand(): T? = tilstandsLogg.get()

inline fun <reified T: BekreftelseTilstandStatus> Bekreftelse.has(): Boolean = tilstand<T>() != null

fun Bekreftelse.sisteTilstand(): BekreftelseTilstandStatus = tilstandsLogg.siste

operator fun Bekreftelse.plus(bekreftelseTilstandStatus: BekreftelseTilstandStatus): Bekreftelse =
    copy(tilstandsLogg = tilstandsLogg + bekreftelseTilstandStatus)
