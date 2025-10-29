package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.DayOfWeek
import java.time.Duration
import java.time.Duration.ofHours
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.ofInstant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

val osloTimezone: ZoneId = ZoneId.of("Europe/Oslo")

private val sameOrPreviousMondayAdjuster = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)

fun sluttTidForBekreftelsePeriode(startTid: Instant, interval: Duration): Instant {
    //Vi legger på 6 timer slik at vi unngår problemer med sommertid/vintertid når vi justerer til mandag
    //https://jira.adeo.no/browse/FAGSYSTEM-403157
    val maalDato = startTid + interval + ofHours(6)
    return sameOrPreviousMondayAdjuster.adjustInto(ofInstant(maalDato, osloTimezone))
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

fun publiseringstidForBekreftelse(sluttTid: Instant, publiseringsOffset: Duration): Instant {
    return sluttTid - publiseringsOffset
}