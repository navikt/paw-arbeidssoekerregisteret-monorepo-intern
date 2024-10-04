package no.nav.paw.bekreftelseutgang.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.health.repository.HealthIndicatorRepository

class ApplicationContext(
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
) {
    companion object {
        fun create(applicationConfig: ApplicationConfig): ApplicationContext {
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val healthIndicatorRepository = HealthIndicatorRepository()

            return ApplicationContext(
                applicationConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
            )
        }
    }
}