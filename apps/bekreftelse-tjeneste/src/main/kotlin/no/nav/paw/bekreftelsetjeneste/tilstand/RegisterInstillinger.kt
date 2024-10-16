package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

fun fristForNesteBekreftelse(forrige: Instant, interval: Duration): Instant {
    val osloTimezone = ZoneId.of("Europe/Oslo")
    return if(LocalDateTime.ofInstant(forrige, osloTimezone).dayOfWeek != DayOfWeek.MONDAY) {
        magicMonday(forrige, interval)
    } else {
        forrige.plus(interval)
    }
}

fun Bekreftelse.gjenstaendeGraceperiode(timestamp: Instant, graceperiode: Duration): Duration {
    val utvidetGjelderTil = tilstand<VenterSvar>()?.timestamp?.plus(graceperiode) ?: gjelderTil.plus(graceperiode)

    return if (timestamp.isAfter(utvidetGjelderTil)) {
        Duration.ZERO
    } else {
        Duration.between(timestamp, utvidetGjelderTil)
    }
}

fun magicMonday(startTime: Instant, interval: Duration): Instant {
    val osloTimezone = ZoneId.of("Europe/Oslo")

    val startDateTime = LocalDateTime.ofInstant(startTime, osloTimezone)
    val endDateTime = startDateTime.plus(interval)

    val lastMondayInInterval = endDateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    return lastMondayInInterval.atZone(osloTimezone).toInstant()
}
