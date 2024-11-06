package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Duration.ofDays
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.ofInstant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

val osloTimezone: ZoneId = ZoneId.of("Europe/Oslo")

private val sameOrPreviousMondayAdjuster = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
private val sameOrNextMondayAdjuster = TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)

fun kalkulerInitiellStartTidForBekreftelsePeriode(
    tidligsteStartTidspunkt: Instant,
    periodeStart: Instant,
    interval: Duration
): Instant {
    //Bruker 'not isBefore' siden den også inkluderer de som er like, i motsetning til 'isAfter'
    if (!periodeStart.isBefore(tidligsteStartTidspunkt)) return periodeStart

    val antattMeldekortStartDato = ofInstant(periodeStart, osloTimezone)
        .let(sameOrPreviousMondayAdjuster::adjustInto)
    val maalDato = ofInstant(tidligsteStartTidspunkt, osloTimezone)
    val dagerIIntervall = interval.toDays()
    val antallDagerIPeriode = ChronoUnit.DAYS.between(antattMeldekortStartDato, maalDato)
    val manglendeDager = dagerIIntervall - (antallDagerIPeriode % dagerIIntervall)
    return when (manglendeDager) {
        0L -> tidligsteStartTidspunkt
        in 1..< dagerIIntervall -> {
            sameOrNextMondayAdjuster
                .adjustInto(ofInstant(tidligsteStartTidspunkt.plus(ofDays(manglendeDager)), osloTimezone))
                .let(LocalDate::from)
                .let(LocalDate::atStartOfDay)
                .atZone(osloTimezone)
                .toInstant()
        }
        else -> return tidligsteStartTidspunkt + ofDays(dagerIIntervall)
    }
}

fun sluttTidForBekreftelsePeriode(startTid: Instant, interval: Duration): Instant {
    val maalDato = startTid + interval
    // For testing i dev, hvor vi kan ha intervaller på 1 dag eller mindre, bryr vi oss ikke om magic monday
    if(interval.toDays() <= 1L) return maalDato
    return sameOrNextMondayAdjuster.adjustInto(ofInstant(maalDato, osloTimezone))
    .let(LocalDate::from)
    .let(LocalDate::atStartOfDay)
    .atZone(osloTimezone)
    .toInstant()
}


fun Bekreftelse.gjenstaendeGraceperiode(timestamp: Instant, graceperiode: Duration): Duration {
    val utvidetGjelderTil = tilstand<VenterSvar>()?.timestamp?.plus(graceperiode) ?: gjelderTil.plus(graceperiode)

    return if (timestamp.isAfter(utvidetGjelderTil)) {
        Duration.ZERO
    } else {
        Duration.between(timestamp, utvidetGjelderTil)
    }
}
