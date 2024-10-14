package no.nav.paw.bekreftelse.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoekerregisteret_api_bekreftelse"

fun MeterRegistry.receiveBekreftelseCounter(type: String, amount: Int = 1) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "receive"),
            Tag.of("channel", "http")
        )
    ).increment()
    bekreftelseCounter(type, "receive", "http", "kafka", amount)
}

fun MeterRegistry.sendBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "send", "http", "kafka", amount)
}

fun MeterRegistry.receiveBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "receive", "kafka", "database", amount)
}

fun MeterRegistry.insertBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "insert", "kafka", "database", amount)
}

fun MeterRegistry.updateBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "update", "kafka", "database", amount)
}

fun MeterRegistry.deleteBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "delete", "kafka", "database", amount)
}

fun MeterRegistry.ignoreBekreftelseHendeleCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, "ignore", "kafka", "none", amount)
}

fun MeterRegistry.bekreftelseCounter(
    type: String,
    action: String,
    source: String,
    target: String,
    amount: Int = 1
) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("type", type),
            Tag.of("action", action),
            Tag.of("source", source),
            Tag.of("target", target)
        )
    ).increment(amount.toDouble())
}

fun MeterRegistry.lagredeBekreftelserTotaltGauge(antallReference: AtomicLong) {
    gauge(
        "${METRIC_PREFIX}_gauge",
        Tags.of(
            Tag.of("action", "lagrede_bekreftelse_hendelser_totalt"),
            Tag.of("channel", "state")
        ),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}
