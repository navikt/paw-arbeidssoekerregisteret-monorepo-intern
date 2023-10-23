package no.nav.paw.kafkakeygenerator

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.config.Autentiseringskonfigurasjon
import no.nav.paw.kafkakeygenerator.config.lastKonfigurasjon

fun main() {
    val autentiseringKonfigurasjon = lastKonfigurasjon<Autentiseringskonfigurasjon>("ktor_server_autentisering.toml")
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    embeddedServer(Netty, port = 8080) {
        konfigurerServer(autentiseringKonfigurasjon, prometheusMeterRegistry)
    }.start(wait = true)
}

private fun Application.konfigurerServer(
    autentiseringKonfigurasjon: Autentiseringskonfigurasjon,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    autentisering(autentiseringKonfigurasjon)
    micrometerMetrics(prometheusMeterRegistry)
    routing {
        konfigurereHelseRuter(prometheusMeterRegistry)
        get("/open-hello") {
            call.respondText("Hello Open World!")
        }
        authenticate(autentiseringKonfigurasjon.name) {
            get("/hello") {
                call.respondText("Hello World!")
            }
        }
    }
}

private fun Routing.konfigurereHelseRuter(prometheusMeterRegistry: PrometheusMeterRegistry) {
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
