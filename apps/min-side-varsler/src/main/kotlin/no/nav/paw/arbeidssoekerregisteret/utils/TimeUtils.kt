package no.nav.paw.arbeidssoekerregisteret.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

fun Instant.tilNesteFredagKl9(): ZonedDateTime = this
    .atZone(ZoneId.of("Europe/Oslo"))
    .with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
    .withHour(9).withMinute(0).withSecond(0).withNano(0)
