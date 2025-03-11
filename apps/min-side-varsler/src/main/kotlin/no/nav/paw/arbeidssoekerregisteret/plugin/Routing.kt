package no.nav.paw.arbeidssoekerregisteret.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.api.docs.routes.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.route.bestillingerRoutes
import no.nav.paw.arbeidssoekerregisteret.route.varselRoutes
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthIndicatorRepository: HealthIndicatorRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    varselService: VarselService,
    bestillingService: BestillingService
) {
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(prometheusMeterRegistry)
        apiDocsRoutes()
        varselRoutes(varselService)
        bestillingerRoutes(bestillingService)
    }
}