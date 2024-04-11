package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.helse.ModuleInfo
import java.util.concurrent.atomic.AtomicLong

private val buildTime = AtomicLong(0)
private val majorVersion = AtomicLong(0)
fun PrometheusMeterRegistry.registerAvroSchemaGauges(info: ModuleInfo) {
    buildTime.set(info.buildTime.toEpochMilli())
    majorVersion.set(info.version.split(".").first().toLong())
    gauge(Names.AVRO_SCHEMA_BUILD_TIME, buildTime) { it.toDouble() }
    gauge(
        Names.AVRO_MAJOR_VERSION,
        Tags.of(Tag.of(Labels.VERSION, info.version)),
        majorVersion
    ) { it.toDouble() }
    gauge(
        Names.AVRO_SCHEMA_AGE,
        buildTime
    ) { bt ->
        (System.currentTimeMillis() - bt.get()).toDouble()
    }
}