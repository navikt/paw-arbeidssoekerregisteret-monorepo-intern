package no.nav.paw.arbeidssokerregisteret.application

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StatsOppholdtilatelse(
    val fra: LocalDate?,
    val til: LocalDate?,
    val type: String
)

fun statsOppholdstilatelse(type: String, fra: String?, til: String?) =
    StatsOppholdtilatelse(
        fra = fra?.let(LocalDate::parse),
        til = til?.let(LocalDate::parse),
        type = type
    )

fun PrometheusMeterRegistry.oppholdstillatelseStats(oppholdtilatelse: StatsOppholdtilatelse?, opplysninger: Collection<Opplysning>) {
    val tags = listOf(
        Tag.of(
            "oppholdstillatelse_type",
            oppholdtilatelse?.type ?: "NA"
        ),
        Tag.of(
            "dager_siden_oppholdstillatelse_start",
            oppholdtilatelse?.fra?.let(::daysBetweenNow)?.let(::asBucket) ?: if (oppholdtilatelse == null) "NA"  else "undefined"
        ),
        Tag.of(
            "dager_til_oppholdstillatelse_stopp",
            oppholdtilatelse?.til?.let(::daysBetweenNow)?.let(::asBucket) ?: if (oppholdtilatelse == null) "NA"  else "undefined"
        ),
        Tag.of(
            "avtale_land",
            (opplysninger.contains(DomeneOpplysning.ErEuEoesStatsborger) || opplysninger.contains(DomeneOpplysning.ErGbrStatsborger)).toString()
        ),
        Tag.of(
            "norsk_statsborger",
            opplysninger.contains(DomeneOpplysning.ErNorskStatsborger).toString()
        )
    ) + opplysningerTags(opplysninger)
    counter("paw_bosatt_vs_oppholds_stats_v1", tags).increment()
}

fun opplysningerTags(opplysninger: Collection<Opplysning>): Collection<Tag> =
    setOf(
        DomeneOpplysning.BosattEtterFregLoven,
        DomeneOpplysning.IkkeBosatt,
        DomeneOpplysning.HarGyldigOppholdstillatelse,
        DomeneOpplysning.UkjentStatusForOppholdstillatelse,
        DomeneOpplysning.OppholdstillatelseUtgaaatt,
        DomeneOpplysning.IngenInformasjonOmOppholdstillatelse
    ).map { Tag.of(it.id, opplysninger.contains(it).toString()) }


fun daysBetweenNow(then: LocalDate): Long {
    val now = LocalDate.now()
    return ChronoUnit.DAYS.between(now, then)
}

fun asBucket(number: Long): String =
    when {
        number < 7 -> number.toString()
        number < 14 -> "[7, 14>"
        number < 28 -> "[14-28>"
        number < 56 -> "[28-56>"
        number < 112 -> "[56-112>"
        number < 224 -> "[112-224>"
        else -> "[224-"
    }