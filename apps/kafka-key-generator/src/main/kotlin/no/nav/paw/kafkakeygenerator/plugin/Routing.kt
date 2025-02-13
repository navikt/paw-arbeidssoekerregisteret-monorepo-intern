package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.kafkakeygenerator.api.internal.mergeDetectorRoutes
import no.nav.paw.kafkakeygenerator.api.recordkey.configureRecordKeyApi
import no.nav.paw.kafkakeygenerator.api.v2.konfigurerApiV2
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.metrics.route.metricsRoutes

fun Application.configureRouting(
    meterRegistry: PrometheusMeterRegistry,
    healthIndicatorRepository: HealthIndicatorRepository,
    kafkaKeysService: KafkaKeysService,
    mergeDetector: MergeDetector
) {
    routing {
        healthRoutes(healthIndicatorRepository)
        metricsRoutes(meterRegistry)
        mergeDetectorRoutes(mergeDetector)
        swaggerUI(path = "docs", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "docs/record-key", swaggerFile = "openapi/record-key-api-spec.yaml")
        konfigurerApiV2(kafkaKeysService)
        configureRecordKeyApi(kafkaKeysService)
    }
}
