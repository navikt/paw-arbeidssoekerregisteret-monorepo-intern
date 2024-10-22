package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

val osloTimezone: ZoneId = ZoneId.of("Europe/Oslo")

fun fristForNesteBekreftelse(forrige: Instant, interval: Duration, now: Instant = Instant.now()): Instant =
    if (forrige.isBefore(now.minus(interval))) {
        // For gamle perioder må vi regne oss opp til neste frist med hensyn til dagens dato
        val startDato = LocalDate.ofInstant(forrige, osloTimezone)
        val dagensDato = LocalDate.ofInstant(now, osloTimezone)
        val antallDagerIPeriode = ChronoUnit.DAYS.between(startDato, dagensDato)
        val antallIntervaller = antallDagerIPeriode / interval.toDays()
        val overskuddsdager = antallDagerIPeriode % interval.toDays()

        magicMonday(forrige.plus(interval.multipliedBy(antallIntervaller).plusDays(overskuddsdager)), interval)
    } else if (LocalDateTime.ofInstant(forrige, osloTimezone).dayOfWeek == DayOfWeek.MONDAY) {
        // Hvis ikke det er gammel periode og forrige frist var en mandag kan vi bare legge til intervallet
        forrige.plus(interval)
    } else {
        // Hvis forrige periode ikke sluttet på mandag må vi finne siste mandag i intervallet
        magicMonday(forrige, interval)
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
    val startDateTime = LocalDateTime.ofInstant(startTime, osloTimezone)
    val endDateTime = startDateTime.plus(interval)

    val lastMondayInInterval = endDateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    return lastMondayInInterval.atZone(osloTimezone).toInstant()
}
