package no.nav.paw.arbeidssoekerregisteret.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.api.docs.routes.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.route.bestillingerRoutes
import no.nav.paw.arbeidssoekerregisteret.route.varselRoutes
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    applicationConfig: ApplicationConfig,
    healthIndicatorRepository: HealthIndicatorRepository,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    varselService: VarselService,
    bestillingService: BestillingService
) {
    install(IgnoreTrailingSlash)
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(prometheusMeterRegistry)
        apiDocsRoutes()
        varselRoutes(varselService)
        bestillingerRoutes(applicationConfig, bestillingService)
    }
}