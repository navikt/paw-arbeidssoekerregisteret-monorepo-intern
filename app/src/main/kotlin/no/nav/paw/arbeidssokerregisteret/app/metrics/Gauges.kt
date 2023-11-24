package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicLong

context(PrometheusMeterRegistry)
fun registerGauge(topic: String, latency: AtomicLong) {
    gauge(
        Names.LATENCY,
        listOf(Tag.of(Labels.TOPIC, topic)),
        latency
    )
}

