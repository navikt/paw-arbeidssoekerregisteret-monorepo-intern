package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.helse.ModuleInfo
import java.util.concurrent.atomic.AtomicLong

private val buildTime = AtomicLong(0)
fun PrometheusMeterRegistry.registerAvroSchemaGauges(info: ModuleInfo) {
    buildTime.set(info.buildTime.toEpochMilli())
    gauge(Names.AVRO_MAJOR_VERSION, info.version.split(".").first().toDouble())
    gauge(Names.AVRO_SCHEMA_BUILD_TIME, buildTime)
    gauge(Names.AVRO_SCHEMA_AGE, buildTime) { bt ->
        (System.currentTimeMillis() - bt.get()).toDouble()
    }
}