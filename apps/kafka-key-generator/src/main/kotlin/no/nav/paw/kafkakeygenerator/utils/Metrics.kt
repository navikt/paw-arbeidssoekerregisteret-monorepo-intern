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

fun MeterRegistry.countRestApiFetched() {
    genericCounter("rest_api", "database", "fetched")
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

fun <T : Number> MeterRegistry.kafkaGauge(
    topic: String,
    partition: Int,
    suffix: String,
    action: String,
    number: T
) {
    gauge(
        "${METRIC_PREFIX}_${suffix}_gauge",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "database"),
            Tag.of("action", action),
            Tag.of("kafka_topic", topic),
            Tag.of("kafka_partition", "$partition")
        ),
        number
    )
}

fun <T : Number> MeterRegistry.kafkaHendelseConflictGauge(
    topic: String,
    count: T
) {
    kafkaGauge(topic, -1, "hendelse", "conflict", count)
}

fun MeterRegistry.kafkaReceivedGauge(
    topic: String,
    partition: Int,
    offset: Long
) {
    kafkaGauge(topic, partition, "hwm", "update", offset)
}
