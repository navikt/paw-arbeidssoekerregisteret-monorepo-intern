package no.nav.paw.dev.proxy.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.dev.proxy.api.route.proxyRoutes
import no.nav.paw.dev.proxy.api.service.ProxyService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    healthIndicatorRepository: HealthIndicatorRepository,
    meterRegistry: PrometheusMeterRegistry,
    proxyService: ProxyService
) {
    install(IgnoreTrailingSlash)
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(meterRegistry)
        proxyRoutes(proxyService)
    }
}
