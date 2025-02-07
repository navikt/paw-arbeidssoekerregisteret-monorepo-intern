package no.nav.paw.dolly.api.plugin

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.dolly.api.route.dollyRoutes
import no.nav.paw.dolly.api.route.swaggerRoutes
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