package no.nav.paw.rapportering.api.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.rapportering.api.Health

fun Routing.healthRoutes(prometheusMeterRegistry: PrometheusMeterRegistry, health: Health) {
    get("/internal/isAlive") {
        val status = health.alive()
        call.respond(status.code, status.message)
    }
    get("/internal/isReady") {
        val status = health.ready()
        call.respond(status.code, status.message)
    }
    get("/internal/metrics") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}