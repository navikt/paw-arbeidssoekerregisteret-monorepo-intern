package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.routes.healthRoutes
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService

context(ApplicationContext)
fun Application.configureRouting(
    healthIndicatorService: HealthIndicatorService,
    meterRegistry: PrometheusMeterRegistry
) {
    routing {
        healthRoutes(healthIndicatorService, meterRegistry)
    }
}
