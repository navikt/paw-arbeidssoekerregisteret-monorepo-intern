package no.nav.paw.arbeidssoekerregisteret.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.model.HealthStatus
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService

fun Route.healthRoutes(
    healthIndicatorService: HealthIndicatorService,
    meterRegistry: PrometheusMeterRegistry
) {

    get("/internal/isAlive") {
        when (val status = healthIndicatorService.getLivenessStatus()) {
            HealthStatus.HEALTHY -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.OK
            ) { status.value }

            else -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.ServiceUnavailable
            ) { status.value }
        }
    }

    get("/internal/isReady") {
        when (val status = healthIndicatorService.getReadinessStatus()) {
            HealthStatus.HEALTHY -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.OK
            ) { status.value }

            else -> call.respondText(
                ContentType.Text.Plain,
                HttpStatusCode.ServiceUnavailable
            ) { status.value }
        }
    }

    get("/internal/metrics") {
        call.respond(meterRegistry.scrape())
    }
}
