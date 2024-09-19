package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

// Felles verdier for bekreftelse.
data object BekreftelseConfig {
    val bekreftelseInterval:Duration = Duration.ofDays(14)
    val gracePeriode: Duration = Duration.ofDays(7)
    val bekreftelseTilgjengeligOffset: Duration = Duration.ofDays(3)
    val varselFoerGracePeriodeUtloept: Duration = gracePeriode.dividedBy(2)
}

fun fristForNesteBekreftelse(forrige: Instant, interval: Duration): Instant {
    // TODO: Finn regler for magic monday og gjør nødvendig justeringer
    val magicMondayAdjuster = MagicMondayAdjuster()
    val zoneId = ZoneId.of("Europe/Oslo")
    return forrige
        .plus(interval)
        .let { LocalDate.ofInstant(it, ZoneId.systemDefault()) }
        //.with(magicMondayAdjuster)
        .plusDays(1)
        .atStartOfDay(zoneId).toInstant()
}

fun gjenstaendeGracePeriode(timestamp: Instant, gjelderTil: Instant): Duration {
    val gracePeriode = BekreftelseConfig.gracePeriode
    val utvidetGjelderTil = gjelderTil.plus(gracePeriode)

    return if (timestamp.isAfter(utvidetGjelderTil)) {
        Duration.ZERO
    } else {
        Duration.between(timestamp, utvidetGjelderTil)
    }
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
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }
}

fun LocalDate.skipForwardIfNotMagicMonday(): LocalDate {
    return this
}
