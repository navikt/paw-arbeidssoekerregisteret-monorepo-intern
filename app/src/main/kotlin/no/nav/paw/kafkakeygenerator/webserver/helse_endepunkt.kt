package no.nav.paw.kafkakeygenerator.webserver

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Routing.konfigurereHelse(prometheusMeterRegistry: PrometheusMeterRegistry) {
    get("/internal/isAlive") {
        call.respondText("ALIVE")
    }
    get("/internal/isReady") {
        call.respondText("READY")
    }
    get("/internal/prometheus") {
        call.respond(prometheusMeterRegistry.scrape())
    }
}
