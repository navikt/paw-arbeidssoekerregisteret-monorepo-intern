package no.nav.paw.dolly.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.dolly.api.routes.dollyRoutes
import no.nav.paw.dolly.api.routes.swaggerRoutes
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext.prometheusMeterRegistry)
        swaggerRoutes()
        dollyRoutes(applicationContext.dollyService)
    }
}