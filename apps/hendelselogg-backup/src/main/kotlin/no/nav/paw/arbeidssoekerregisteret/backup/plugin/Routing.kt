package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.apiDocsRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.health.isDatabaseReady
import no.nav.paw.arbeidssoekerregisteret.backup.health.isKafkaConsumerReady
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.metrics.route.metricsRoutes
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering
import no.nav.paw.startup.startupRoute

fun Application.configureRouting(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(IgnoreTrailingSlash)
        routing {
            startupRoute(
                { isKafkaConsumerReady(hendelseConsumerWrapper) },
                { isDatabaseReady(dataSource) },
            )
            healthRoutes(healthIndicatorRepository)
            metricsRoutes(prometheusMeterRegistry)
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
}