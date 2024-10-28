package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import org.slf4j.LoggerFactory

fun Routing.konfigurereHelse(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    mergeDetector: MergeDetector
) {
    val mergeLogger = LoggerFactory.getLogger("MergeDetector")
    get("/internal/isAlive") {
        call.respondText("ALIVE")
    }
    get("/internal/isReady") {
        call.respondText("READY")
    }
    get("/internal/metrics") {
        call.respond(prometheusMeterRegistry.scrape())
    }
    get("internal/mergeDetector") {
        call.respondText(
            mergeDetector
                .findMerges(1000)
                .map { "Number of pending merges: ${it.size } "}
                .onRight { mergeLogger.info(it) }
                .onLeft { mergeLogger.error("Error: ${it.system}:${it.code}", it.exception) }
                .fold( { "Error: ${it.system}:${it.code}" }, { it } )
        )
    }
}
