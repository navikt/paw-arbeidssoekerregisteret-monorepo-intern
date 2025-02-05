package no.nav.paw.metrics.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Route.metricsRoutes(
    meterRegistry: PrometheusMeterRegistry,
) {
    get("/internal/metrics") {
        call.respond(meterRegistry.scrape())
    }
}