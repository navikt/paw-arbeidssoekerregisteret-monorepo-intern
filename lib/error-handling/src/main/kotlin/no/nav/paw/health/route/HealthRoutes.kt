package no.nav.paw.health.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.getAggregatedStatus
import no.nav.paw.health.repository.HealthIndicatorRepository

fun Route.healthRoutes(
    healthIndicatorRepository: HealthIndicatorRepository = HealthIndicatorRepository(),
) {

    get("/internal/isAlive") {
        val livenessIndicators = healthIndicatorRepository.getLivenessIndicators()
        when (val status = livenessIndicators.getAggregatedStatus()) {
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
        val readinessIndicators = healthIndicatorRepository.getReadinessIndicators()
        when (val status = readinessIndicators.getAggregatedStatus()) {
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
}
