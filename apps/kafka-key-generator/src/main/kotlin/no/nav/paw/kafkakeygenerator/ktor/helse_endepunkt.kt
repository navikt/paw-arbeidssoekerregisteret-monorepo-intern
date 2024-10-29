package no.nav.paw.kafkakeygenerator.ktor

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.Either
import no.nav.paw.kafkakeygenerator.Failure
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.atomic.AtomicReference

private val task = AtomicReference<CompletableFuture<Either<Failure, Long>>?>(null)

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
   /* get("internal/mergeDetector") {
        val t = task.get()
        if (t == null) {
            task.set(supplyAsync { runBlocking { mergeDetector.findMerges(900) } })
            call.respondText("Merge detection started")
        } else {
            if (t.isDone) {
                call.respondText(
                    t.get()
                        .map { "Number of pending merges: $it "}
                        .onRight { mergeLogger.info(it) }
                        .onLeft { mergeLogger.error("Error: ${it.system}:${it.code}", it.exception) }
                        .fold( { "Error: ${it.system}:${it.code}" }, { it } )
                )
            } else {
                call.respondText("Merge detection in progress")
            }
        }
    }*/
}
