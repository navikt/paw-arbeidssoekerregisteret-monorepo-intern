package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
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
        }
    }.start(wait = false)
}