package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.getMockResponse
import no.nav.paw.arbeidssoekerregisteret.backup.health.configureHealthRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.health.installMetrics
import org.apache.kafka.clients.consumer.Consumer

fun initKtor(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    consumer: Consumer<*, *>
) {
    embeddedServer(Netty, port = 8080) {
        installMetrics(consumer, prometheusMeterRegistry)
        routing {
            swaggerUI(path = "docs/brukerstoette", swaggerFile = "openapi/Brukerstoette.yaml")
            configureHealthRoutes(prometheusMeterRegistry)
            post("/api/v1/arbeidssoeker/detaljer") {
                val request = call.receive<DetaljerRequest>()
                call.respond(getMockResponse())
            }
        }
    }.start(wait = false)
}