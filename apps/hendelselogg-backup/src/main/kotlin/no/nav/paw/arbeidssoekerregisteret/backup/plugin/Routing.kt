package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Application.configureRouting(
    meterRegistry: PrometheusMeterRegistry,
    brukerstoetteService: BrukerstoetteService
) {
    install(IgnoreTrailingSlash)
    routing {
        healthRoutes()
        metricsRoutes(meterRegistry)
        apiDocsRoutes()
        route("/api/v1") {
            if (currentRuntimeEnvironment is ProdGcp) {
                autentisering(AzureAd) {
                    brukerstoetteRoutes(brukerstoetteService)
                }
            } else {
                brukerstoetteRoutes(brukerstoetteService)
            }
        }
    }
}