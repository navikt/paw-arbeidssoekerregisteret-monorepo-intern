package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.health

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.installMetrics(
    binders: List<MeterBinder>,
    prometheusRegistry: PrometheusMeterRegistry
) {
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        ) + binders
    }
}

fun Routing.configureHealthRoutes(
    prometheusRegistry: PrometheusMeterRegistry
) {
    get("/internal/metrics") {
        call.respondText(prometheusRegistry.scrape())
    }
    get("/internal/isAlive") {
        call.respondText("ALIVE")
    }
    get("/internal/isReady") {
        call.respondText("READY")
    }
}

