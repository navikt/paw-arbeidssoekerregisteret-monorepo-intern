package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import java.time.Duration
import java.time.Instant
import java.util.*

@JvmRecord
data class Bekreftelse(
    val tilstandsLogg: BekreftelseTilstandsLogg,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)

inline fun <reified T: BekreftelseTilstand> Bekreftelse.tilstand(): T? = tilstandsLogg.get()

inline fun <reified T: BekreftelseTilstand> Bekreftelse.has(): Boolean = tilstand<T>() != null

fun Bekreftelse.sisteTilstand(): BekreftelseTilstand = tilstandsLogg.siste

operator fun Bekreftelse.plus(bekreftelseTilstand: BekreftelseTilstand): Bekreftelse =
    copy(tilstandsLogg = tilstandsLogg + bekreftelseTilstand)

fun opprettFoersteBekreftelse(
    periode: PeriodeInfo,
    interval: Duration,
    currentTime: Instant
): Bekreftelse {
    val start = lastOf(periode.startet, currentTime - interval)
    return Bekreftelse(
        BekreftelseTilstandsLogg(IkkeKlarForUtfylling(periode.startet), emptyList()),
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = periode.startet,
        gjelderTil = fristForNesteBekreftelse(start, interval, periode.startet+interval)
    )
}

fun lastOf(a: Instant, b: Instant): Instant = if (a.isAfter(b)) a else b


fun NonEmptyList<Bekreftelse>.opprettNesteTilgjengeligeBekreftelse(
    tilgjengeliggjort: Instant,
    interval: Duration,
): Bekreftelse {
    val sisteBekreftelse = maxBy { it.gjelderTil }
    return Bekreftelse(
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = sisteBekreftelse.gjelderTil,
        gjelderTil = fristForNesteBekreftelse(sisteBekreftelse.gjelderTil, interval, tilgjengeliggjort),
        tilstandsLogg = BekreftelseTilstandsLogg(
            siste = KlarForUtfylling(tilgjengeliggjort),
            tidligere = emptyList()
        )
    )
}
