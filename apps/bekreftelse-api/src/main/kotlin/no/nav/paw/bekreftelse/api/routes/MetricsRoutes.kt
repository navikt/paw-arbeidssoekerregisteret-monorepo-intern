package no.nav.paw.bekreftelse.api.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.metricsRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/metrics") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}