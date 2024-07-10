package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry

private const val METRIC_PREFIX = "paw_arbeidssoeker_opplysninger_aggregering"

fun MeterRegistry.tellAntallMottatteOpplysninger() {
    counter(
        "${METRIC_PREFIX}_antall_mottatte_opplysninger_total",
    ).increment()
}
