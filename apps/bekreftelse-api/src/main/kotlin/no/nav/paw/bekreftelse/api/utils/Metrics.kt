package no.nav.paw.bekreftelse.api.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.util.concurrent.atomic.AtomicLong

private const val METRIC_PREFIX = "paw_arbeidssoekerregisteret_api_bekreftelse"

fun MeterRegistry.mottaBekreftelseHttpCounter() {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "mottatt_bekreftelse"),
            Tag.of("channel", "http")
        )
    ).increment()
}

fun MeterRegistry.sendeBekreftelseKafkaCounter() {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "sendt_bekreftelse"),
            Tag.of("channel", "kafka")
        )
    ).increment()
}

fun MeterRegistry.mottattBekreftelseHendelseKafkaCounter(hendelseType: String) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "mottatt_bekreftelse_hendelse"),
            Tag.of("type", hendelseType),
            Tag.of("channel", "kafka")
        )
    ).increment()
}

fun MeterRegistry.lagreBekreftelseHendelseCounter(hendelseType: String, amount: Long = 1) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "lagret_bekreftelse_hendelse"),
            Tag.of("type", hendelseType),
            Tag.of("channel", "state")
        )
    ).increment(amount.toDouble())
}

fun MeterRegistry.slettetBekreftelseHendelseCounter(hendelseType: String, amount: Long = 1) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "slettet_bekreftelse_hendelse"),
            Tag.of("type", hendelseType),
            Tag.of("channel", "state")
        )
    ).increment(amount.toDouble())
}

fun MeterRegistry.ignorertBekreftelseHendeleCounter(hendelseType: String) {
    counter(
        "${METRIC_PREFIX}_counter",
        Tags.of(
            Tag.of("action", "ignorert_bekreftelse_hendelse"),
            Tag.of("type", hendelseType),
            Tag.of("channel", "state")
        )
    ).increment()
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
