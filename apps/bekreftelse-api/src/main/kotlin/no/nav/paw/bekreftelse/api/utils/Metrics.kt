package no.nav.paw.bekreftelse.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoekerregisteret_api_bekreftelse"

fun MeterRegistry.mottaBekreftelseCounter() {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "mottatt_bekreftelse_http")
        )
    ).increment()
}

fun MeterRegistry.sendeBekreftelseCounter() {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "sendt_bekreftelse_kafka")

        )
    ).increment()
}

fun MeterRegistry.lagredeBekreftelserGauge(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_gauge",
        Tags.of(
            Tag.of("action", "lagrede_bekreftelser_internt")

        ),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}
