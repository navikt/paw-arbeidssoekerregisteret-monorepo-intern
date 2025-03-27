package no.nav.paw.bekreftelse.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.api.docs.routes.apiDocsRoutes
import no.nav.paw.bekreftelse.api.route.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.service.AuthorizationService
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    meterRegistry: PrometheusMeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    authorizationService: AuthorizationService,
    bekreftelseService: BekreftelseService
) {
    install(IgnoreTrailingSlash)
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(meterRegistry)
        apiDocsRoutes()
        bekreftelseRoutes(authorizationService, bekreftelseService)
    }
}
