package no.nav.paw.kafkakeygenerator.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags

private const val METRIC_PREFIX = "paw_kafka_key_generator"

fun MeterRegistry.genericCounter(
    source: String,
    target: String,
    action: String
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", source),
            Tag.of("target", target),
            Tag.of("action", action)
        )
    ).increment()
}

fun MeterRegistry.countRestApiReceived() {
    genericCounter("rest_api", "database", "received")
}

fun MeterRegistry.countRestApiFetch() {
    genericCounter("rest_api", "database", "fetch")
}

fun MeterRegistry.countRestApiInserted() {
    genericCounter("rest_api", "database", "inserted")
}

fun MeterRegistry.countRestApiFailed() {
    genericCounter("rest_api", "database", "failed")
}

fun MeterRegistry.countKafkaReceived() {
    genericCounter("kafka", "database", "received")
}

fun MeterRegistry.countKafkaProcessed() {
    genericCounter("kafka", "database", "processed")
}

fun MeterRegistry.countKafkaIgnored() {
    genericCounter("kafka", "database", "ignored")
}

fun MeterRegistry.countKafkaInserted() {
    genericCounter("kafka", "database", "inserted")
}

fun MeterRegistry.countKafkaUpdated() {
    genericCounter("kafka", "database", "updated")
}

fun MeterRegistry.countKafkaVerified() {
    genericCounter("kafka", "database", "verified")
}

fun MeterRegistry.countKafkaFailed() {
    genericCounter("kafka", "database", "failed")
}
