package no.nav.paw.kafkakeymaintenance

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes

fun initKtor(
    healthIndicatorRepository: HealthIndicatorRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    vararg additionalRoutes: Route.() -> Unit
) =
    embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusMeterRegistry
            meterBinders = listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics()
            )
        }
        routing {
            healthRoutes(healthIndicatorRepository)
            prometheusMeterRegistry?.let {
                get("/internal/metrics") {
                    call.respondText(prometheusMeterRegistry.scrape())
                }
            }
            additionalRoutes.forEach { it() }
        }
    }