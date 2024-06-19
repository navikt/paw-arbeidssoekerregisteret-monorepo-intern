package no.nav.paw.arbeidssoekerregisteret.backup.health

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.HwmRebalanceListener
import org.apache.kafka.clients.consumer.Consumer

fun Application.installMetrics(
    consumer: Consumer<*, *>,
    prometheusRegistry: PrometheusMeterRegistry
) {
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        meterBinders = listOf(
            KafkaClientMetrics(consumer),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        )
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

