package no.nav.paw.bekretelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

data class RapporteringsKonfigurasjon(
    val rapporteringTilgjengligOffset: Duration,
    val varselFoerUtloepAvGracePeriode: Duration
)

fun fristForNesteRapportering(forrige: Instant, interval: Duration): Instant {
    val magicMondayAdjuster = MagicMondayAdjuster()
    val zoneId = ZoneId.of("Europe/Oslo")
    return forrige
        .plus(interval)
        .let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
        .with(magicMondayAdjuster)
        .plus(Duration.ofDays(1))
        .atStartOfDay(zoneId).toInstant()
}

class MagicMondayAdjuster: TemporalAdjuster {
    override fun adjustInto(temporal: java.time.temporal.Temporal): java.time.temporal.Temporal {
        val internalAdjuster = TemporalAdjusters.next(DayOfWeek.MONDAY)
        val internalTemporal = when (temporal) {
            is LocalDate -> temporal
            is Instant -> LocalDate.ofInstant(temporal, ZoneId.systemDefault())
            else -> LocalDate.from(temporal)
        }
        return internalTemporal
            .with(internalAdjuster)
            .skipForwardIfNotMagicMonday()
            .plus(Duration.ofDays(1))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }
}

fun LocalDate.skipForwardIfNotMagicMonday(): LocalDate {
    return this
}
