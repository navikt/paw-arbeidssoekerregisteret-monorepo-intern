package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.kafkakeygenerator.Applikasjon
import no.nav.paw.kafkakeygenerator.api.recordkey.configureRecordKeyApi
import no.nav.paw.kafkakeygenerator.api.v2.konfigurerApiV2
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.kafkakeygenerator.ktor.konfigurereMetrics
import no.nav.paw.kafkakeygenerator.merge.MergeDetector

fun Application.configureRouting(
    autentiseringKonfigurasjon: AuthenticationConfig,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    applikasjon: Applikasjon,
    mergeDetector: MergeDetector
) {
    routing {
        healthRoutes(healthIndicatorRepository)
        konfigurereMetrics(
            prometheusMeterRegistry = prometheusMeterRegistry,
            mergeDetector = mergeDetector
        )
        konfigurerApiV2(autentiseringKonfigurasjon, applikasjon)
        configureRecordKeyApi(autentiseringKonfigurasjon, applikasjon)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "docs/record-key", swaggerFile = "openapi/record-key-api-spec.yaml")
    }
}
