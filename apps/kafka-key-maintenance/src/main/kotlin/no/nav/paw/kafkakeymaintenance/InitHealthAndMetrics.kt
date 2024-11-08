package no.nav.paw.kafkakeymaintenance

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes

fun initKtor(
    healthIndicatorRepository: HealthIndicatorRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry? = null,
    vararg additionalRoutes: Route.() -> Unit
) =
    embeddedServer(Netty, port = 8080) {
        routing {
            healthRoutes(healthIndicatorRepository)
            prometheusMeterRegistry?.let {
                get("/internal/metrics") {
                    call.respondText(prometheusMeterRegistry.scrape())
                }
            }
            additionalRoutes.forEach { it() }
        }
    }