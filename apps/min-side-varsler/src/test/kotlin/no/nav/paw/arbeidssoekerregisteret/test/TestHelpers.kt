package no.nav.paw.arbeidssoekerregisteret.test

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

val Int.dager: Duration get() = Duration.ofDays(this.toLong())
val Int.timer: Duration get() = Duration.ofHours(this.toLong())
private val tidFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private val isoTidFormatter = DateTimeFormatter.ISO_DATE_TIME
val String.tid: Instant
    get() = LocalDateTime.parse(this, tidFormatter)
        .atZone(ZoneId.of("Europe/Oslo"))
        .toInstant()
val String.isoTid: Instant
    get() = LocalDateTime.parse(this, isoTidFormatter)
        .atZone(ZoneId.of("Europe/Oslo"))
        .toInstant()

fun randomFnr(
    minAlder: Year = Year.now().minusYears(18),
    maksAlder: Year = Year.now().minusYears(100)
): String {
    val formatter = DateTimeFormatter.ofPattern("ddMMyy")
    val randomAlder = Random.nextInt(maksAlder.value, minAlder.value)
    val randomEpochSecond = Random.nextLong(0, Instant.now().epochSecond)
    val randomFoedselsdag = LocalDate.ofInstant(Instant.ofEpochSecond(randomEpochSecond), ZoneOffset.UTC)
        .withYear(randomAlder)
    return formatter.format(randomFoedselsdag) + Random.nextInt(10000, 99999).toString()
}
