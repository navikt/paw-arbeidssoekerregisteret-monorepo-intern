package no.nav.paw.bekreftelsetjeneste.tilstand

import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.collections.PawNonEmptyList
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

const val MAKS_ANTALL_UTSTEENDE_BEKREFTELSER: Int = 100
private val maksAntallLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.maksAntallLogger")
fun Bekreftelse.erKlarForUtfylling(now: Instant, tilgjengeligOffset: Duration): Boolean =
    when (sisteTilstand()) {
        is IkkeKlarForUtfylling -> now.isAfter(gjelderTil.minus(tilgjengeligOffset))
        else -> false
    }


fun Bekreftelse.harFristUtloept(now: Instant, tilgjengeligOffset: Duration): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is KlarForUtfylling -> now.isAfter(gjeldeneTilstand.timestamp.plus(tilgjengeligOffset))
        else -> false
    }


fun Bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(now: Instant, varselFoerGraceperiodeUtloept: Duration): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is VenterSvar -> !has<GracePeriodeVarselet>() && now.isAfter(
            gjeldeneTilstand.timestamp.plus(
                varselFoerGraceperiodeUtloept
            )
        )

        else -> false
    }

fun Bekreftelse.harGraceperiodeUtloept(now: Instant, graceperiode: Duration): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is VenterSvar -> now.isAfter(gjeldeneTilstand.timestamp.plus(graceperiode))
        is GracePeriodeVarselet -> tilstandsLogg.get<VenterSvar>()?.timestamp?.let { ts ->  now.isAfter(ts.plus(graceperiode)) } ?: false
        else -> false
    }


fun PawNonEmptyList<Bekreftelse>.skalOppretteNyBekreftelse(
    wallclock: WallClock,
    interval: Duration,
    tilgjengeligOffset: Duration
): Boolean =
    (size < MAKS_ANTALL_UTSTEENDE_BEKREFTELSER)
        .also { underGrense ->
            if (!underGrense) {
                maksAntallLogger.warn("Maks antall bekreftelser er nådd!")
            }
        } &&
            maxBy { it.gjelderTil }
                .let {
                    wallclock.value.isAfter(it.gjelderTil.plus(interval.minus(tilgjengeligOffset)))
                }




