package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicLong

context(PrometheusMeterRegistry)
fun registerLatencyGauge(topic: String, partition: Int, latency: AtomicLong) {
    gauge(
        Names.LATENCY,
        listOf(
            Tag.of(Labels.TOPIC, topic),
            Tag.of(Labels.PARTITION, partition.toString())
        ),
        latency
    )
}

