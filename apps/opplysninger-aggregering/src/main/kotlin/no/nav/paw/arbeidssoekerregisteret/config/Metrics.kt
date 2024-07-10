package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoeker_opplysninger_aggregering"

fun MeterRegistry.tellMottatteOpplysninger() {
    counter(
        "${METRIC_PREFIX}_antall_mottatte_opplysninger_total",
    ).increment()
}

fun MeterRegistry.antallLagredeOpplysningerTotal(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_opplysninger_total",
        Tags.empty(),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}

fun MeterRegistry.antallLagredeOpplysningerSumPerPeriode(timestamp: Instant, antallReference: AtomicLong) {
    val zonedDateTime = timestamp.atZone(ZoneId.systemDefault())
    gauge(
        "${METRIC_PREFIX}_antall_lagrede_opplysninger_sum_per_periode",
        Tags.of(
            Tag.of("minute", "${zonedDateTime.minute}")
        ),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}
