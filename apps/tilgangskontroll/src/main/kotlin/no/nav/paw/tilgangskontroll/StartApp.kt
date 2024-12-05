package no.nav.paw.tilgangskontroll

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("tilgangskontroll")
fun main() {
    logger.info("Starter tilgangskontroll...")
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/internal/isAlive") {
                call.respondText("ALIVE")
            }
            get("/internal/isReady") {
                call.respondText("READY")
            }
            get("/internal/metrics") {
                call.respondText(prometheusMeterRegistry.scrape())
            }
        }
    }.start(wait = true)
    logger.info("Avslutter tilgangskontroll...")
}