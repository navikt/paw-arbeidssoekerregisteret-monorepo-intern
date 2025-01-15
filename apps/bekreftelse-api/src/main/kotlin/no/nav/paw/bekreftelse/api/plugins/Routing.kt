package no.nav.paw.bekreftelse.api.plugins


import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.routes.metricsRoutes
import no.nav.paw.bekreftelse.api.routes.swaggerRoutes
import no.nav.paw.health.route.healthRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext.prometheusMeterRegistry)
        swaggerRoutes()
        bekreftelseRoutes(applicationContext.authorizationService, applicationContext.bekreftelseService)
    }
}
