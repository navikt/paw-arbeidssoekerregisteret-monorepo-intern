package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import no.nav.paw.collections.PawNonEmptyList
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

inline fun <reified T: BekreftelseTilstandStatus> Bekreftelse.tilstand(): T? = tilstandsLogg.get()

inline fun <reified T: BekreftelseTilstandStatus> Bekreftelse.has(): Boolean = tilstand<T>() != null

fun Bekreftelse.sisteTilstand(): BekreftelseTilstandStatus = tilstandsLogg.siste

operator fun Bekreftelse.plus(bekreftelseTilstandStatus: BekreftelseTilstandStatus): Bekreftelse =
    copy(tilstandsLogg = tilstandsLogg + bekreftelseTilstandStatus)

fun opprettFoersteBekreftelse(
    tidligsteStartTidspunktForBekreftelse: Instant,
    periode: PeriodeInfo,
    interval: Duration
): Bekreftelse {
    val start = kalkulerInitiellStartTidForBekreftelsePeriode(
        tidligsteStartTidspunkt = tidligsteStartTidspunktForBekreftelse,
        periodeStart = periode.startet,
        interval = interval
    )
    return Bekreftelse(
        BekreftelseTilstandsLogg(IkkeKlarForUtfylling(periode.startet), emptyList()),
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = start,
        gjelderTil = sluttTidForBekreftelsePeriode(start, interval)
    )
}

fun PawNonEmptyList<Bekreftelse>.opprettNesteTilgjengeligeBekreftelse(
    tilgjengeliggjort: Instant,
    interval: Duration,
): Bekreftelse {
    val sisteBekreftelse = maxBy { it.gjelderTil }
    return Bekreftelse(
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = sisteBekreftelse.gjelderTil,
        gjelderTil = sluttTidForBekreftelsePeriode(sisteBekreftelse.gjelderTil, interval),
        tilstandsLogg = BekreftelseTilstandsLogg(
            siste = KlarForUtfylling(tilgjengeliggjort),
            tidligere = emptyList()
        )
    )
}
