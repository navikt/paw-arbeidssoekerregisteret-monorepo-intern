package no.nav.paw.kafka.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags

private const val METRIC_PREFIX = "paw_kafka_consumer_hwm"

fun MeterRegistry.kafkaHwmUpdateGauge(
    topic: String,
    partition: Int,
    offset: Long
) {
    gauge(
        "${METRIC_PREFIX}_gauge",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "database"),
            Tag.of("action", "update"),
            Tag.of("kafka_topic", topic),
            Tag.of("kafka_partition", "$partition")
        ),
        offset
    )
}
