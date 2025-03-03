package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthIndicatorRepository: HealthIndicatorRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(prometheusMeterRegistry)
    }
}