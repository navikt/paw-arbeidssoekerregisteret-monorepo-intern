package no.nav.paw.bekreftelse.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoekerregisteret_api_bekreftelse"

fun MeterRegistry.receiveBekreftelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, ReceiveAction, "http", "kafka", amount)
}

fun MeterRegistry.sendBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, SendAction, "http", "kafka", amount)
}

fun MeterRegistry.receiveBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, ReceiveAction, "kafka", "database", amount)
}

fun MeterRegistry.insertBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, CreateAction, "kafka", "database", amount)
}

fun MeterRegistry.updateBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, UpdateAction, "kafka", "database", amount)
}

fun MeterRegistry.deleteBekreftelseHendelseCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, DeleteAction, "kafka", "database", amount)
}

fun MeterRegistry.ignoreBekreftelseHendeleCounter(type: String, amount: Int = 1) {
    bekreftelseCounter(type, IgnoreAction, "kafka", "none", amount)
}

fun MeterRegistry.bekreftelseCounter(
    type: String,
    action: TelemetryAction,
    source: String,
    target: String,
    amount: Int = 1
) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("type", type),
            Tag.of("action", action.action),
            Tag.of("source", source),
            Tag.of("target", target)
        )
    ).increment(amount.toDouble())
}

fun MeterRegistry.bekreftelseGauge(
    type: String,
    action: TelemetryAction,
    source: String,
    target: String,
    amount: AtomicLong
) {
    gauge(
        "${METRIC_PREFIX}_gauge",
        Tags.of(
            Tag.of("type", type),
            Tag.of("action", action.action),
            Tag.of("source", source),
            Tag.of("target", target)
        ),
        amount
    ) {
        amount.get().toDouble()
    }
}
