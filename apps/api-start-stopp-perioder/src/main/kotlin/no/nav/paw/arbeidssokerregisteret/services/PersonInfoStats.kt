package no.nav.paw.arbeidssokerregisteret.services

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun PrometheusMeterRegistry.personInfoStats(person: Person, opplysninger: Collection<Opplysning>) {
    val tags = listOf(
        Tag.of("bosatt", opplysninger.contains(DomeneOpplysning.BosattEtterFregLoven).toString()),
        Tag.of(
            "oppholdstillatelse",
            opplysninger.find { it.id.contains("Oppholdstillatelse", ignoreCase = true) }?.id ?: "null"
        ),
        Tag.of(
            "dager_siden_oppholdstillatelse_start",
            person.opphold.firstOrNull()?.oppholdFra?.let(LocalDate::parse)?.let(::daysBetweenNow)?.let(::asBucket) ?: "null"
        ),
        Tag.of(
            "dager_til_oppholdstillatelse_stopp",
            person.opphold.firstOrNull()?.oppholdTil?.let(LocalDate::parse)?.let(::daysBetweenNow)?.let(::asBucket) ?: "null"
        ),
        Tag.of(
            "avtale_land", (opplysninger.contains(DomeneOpplysning.ErEuEoesStatsborger) || opplysninger.contains(DomeneOpplysning.ErGbrStatsborger)).toString()
        )
    )
    counter("paw_arbeidssoeker_inngang_opplysninger", tags).increment()
}

fun daysBetweenNow(then: LocalDate): Long {
    val now = LocalDate.now()
    return ChronoUnit.DAYS.between(now, then)
}

fun asBucket(number: Long): String =
    when {
        number < 14 -> "[0, 14>"
        number < 28 -> "[14-28>"
        number < 56 -> "[28-56>"
        number < 112 -> "[56-112>"
        number < 224 -> "[112-224>"
        else -> "[224-"
    }