package no.nav.paw.kafkakeygenerator.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags

private const val METRIC_PREFIX = "paw_kafka_key_generator"

fun MeterRegistry.genericCounter(
    suffix: String,
    source: String,
    target: String,
    action: String
) {
    counter(
        "${METRIC_PREFIX}_antall_${suffix}",
        Tags.of(
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        )
    ).increment()
}

fun MeterRegistry.countReceivedEvents() {
    genericCounter("hendelser", "kafka", "database", "received")
}

fun MeterRegistry.countProcessedEvents() {
    genericCounter("hendelser", "kafka", "database", "processed")
}

fun MeterRegistry.countIgnoredEvents() {
    genericCounter("hendelser", "kafka", "database", "ignored")
}
