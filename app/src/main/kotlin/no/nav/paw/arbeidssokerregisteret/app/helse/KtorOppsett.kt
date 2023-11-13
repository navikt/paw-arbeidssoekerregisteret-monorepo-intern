package no.nav.paw.arbeidssokerregisteret.app.helse

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun initKtor(
    prometheusRegistry: PrometheusMeterRegistry,
    helse: Helse
): ApplicationEngine {
    return embeddedServer(Netty, port = 8080) {
        routing {
            get("/isReady") {
                val status = helse.ready()
                call.respond(status.code, status.message)
            }
            get("/isAlive") {
                val alive = helse.alive()
                call.respond(alive.code, alive.message)
            }
            get("/metrics") {
                call.respond(prometheusRegistry.scrape())
            }
        }
    }
}
