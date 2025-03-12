package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.streamLogger
import no.nav.paw.arbeidssokerregisteret.profilering.ModuleInfo
import no.nav.paw.arbeidssokerregisteret.profilering.getModuleInfo
import java.util.concurrent.atomic.AtomicLong

private val buildTime = AtomicLong(0)
private val majorVersion = AtomicLong(0)

fun PrometheusMeterRegistry.registerAvroSchemaGauges(metricName: String, info: ModuleInfo) {
    buildTime.set(info.buildTime.toEpochMilli())
    majorVersion.set(info.version.split(".").first().toLong())
    gauge(
        metricName,
        Tags.of(
            Tag.of("buildTimestamp", info.buildTime.toString()),
            Tag.of("groupName", info.group),
            Tag.of("artifactId", info.name),
            Tag.of("version", info.version),
        ),
        buildTime
    ) { bt ->
        streamLogger.info("AvroSchemaGauge for $metricName: $bt")
        (System.currentTimeMillis() - bt.get()).toDouble()
    }
    streamLogger.info("Registerte AvroSchemaGauge {} => {}", metricName, info)
}

fun PrometheusMeterRegistry.registerMainAvroSchemaGauges(): ModuleInfo? =
    getModuleInfo("avro-schema")
        ?.also { registerAvroSchemaGauges("paw_main_avro_schema", it) }