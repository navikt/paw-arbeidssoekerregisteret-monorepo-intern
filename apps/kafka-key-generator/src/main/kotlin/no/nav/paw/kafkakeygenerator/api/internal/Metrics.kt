package no.nav.paw.kafkakeygenerator.api.internal

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

private val task = AtomicReference<CompletableFuture<Either<Failure, Long>>?>(null)

fun Routing.konfigurereMetrics(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    mergeDetector: MergeDetector
) {
    val mergeLogger = LoggerFactory.getLogger("MergeDetector")
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
