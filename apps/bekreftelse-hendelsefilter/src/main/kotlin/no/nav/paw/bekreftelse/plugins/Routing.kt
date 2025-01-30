package no.nav.paw.bekreftelse.plugins


import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.bekreftelse.context.ApplicationContext
import no.nav.paw.bekreftelse.routes.metricsRoutes
import no.nav.paw.health.route.healthRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext.prometheusMeterRegistry)
    }
}
