package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.healthRoutes(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/isAlive") {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }
    get("/internal/isReady") {
        call.respondText("READY", ContentType.Text.Plain)
    }
    get("/internal/metrics") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}
