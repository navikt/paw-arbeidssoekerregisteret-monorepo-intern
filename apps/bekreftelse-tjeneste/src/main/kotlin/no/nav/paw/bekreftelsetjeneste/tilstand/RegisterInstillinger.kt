package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

fun fristForNesteBekreftelse(forrige: Instant, interval: Duration): Instant {
    return forrige.plus(interval)
}

fun Bekreftelse.gjenstaendeGraceperiode(timestamp: Instant, graceperiode: Duration): Duration {
    val utvidetGjelderTil = tilstand<VenterSvar>()?.timestamp?.plus(graceperiode) ?: gjelderTil.plus(graceperiode)

    return if (timestamp.isAfter(utvidetGjelderTil)) {
        Duration.ZERO
    } else {
        Duration.between(timestamp, utvidetGjelderTil)
    }
}

// TODO: Finn regler for magic monday og gjør nødvendig justeringer
/*fun fristForNesteBekreftelseWithMagicMonday(forrige: Instant, interval: Duration): Instant {

    val magicMondayAdjuster = MagicMondayAdjuster()
    val zoneId = ZoneId.of("Europe/Oslo")
    return forrige
        .plus(interval)
        .let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
        .with(magicMondayAdjuster)
        .plusDays(1)
        .atStartOfDay(zoneId).toInstant()
}*/

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
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }
}

fun LocalDate.skipForwardIfNotMagicMonday(): LocalDate {
    return this
}
