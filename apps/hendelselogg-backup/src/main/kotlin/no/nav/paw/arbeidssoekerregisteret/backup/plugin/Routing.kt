package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    meterRegistry: PrometheusMeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    authorizationService: AuthorizationService,
    brukerstoetteService: BrukerstoetteService
) {
    install(IgnoreTrailingSlash)
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(meterRegistry)
        apiDocsRoutes()
        brukerstoetteRoutes(authorizationService, brukerstoetteService)
    }
}