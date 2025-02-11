package no.nav.paw.bekreftelse.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.paw.api.docs.routes.apiDocsRoutes
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.route.bekreftelseRoutes
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(applicationContext: ApplicationContext) {
    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext.prometheusMeterRegistry)
        apiDocsRoutes()
        bekreftelseRoutes(applicationContext.authorizationService, applicationContext.bekreftelseService)
    }
}
