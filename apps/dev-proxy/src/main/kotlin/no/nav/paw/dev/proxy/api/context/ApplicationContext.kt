package no.nav.paw.dev.proxy.api.context

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.dev.proxy.api.config.SERVER_CONFIG
import no.nav.paw.dev.proxy.api.config.ServerConfig
import no.nav.paw.dev.proxy.api.service.ProxyService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.serialization.jackson.configureJackson

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val proxyService: ProxyService
) {
    companion object {
        fun create(): ApplicationContext {
            return ApplicationContext(
                serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG),
                prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                healthIndicatorRepository = HealthIndicatorRepository(),
                proxyService = ProxyService(createHttpClient {
                    install(ContentNegotiation) {
                        jackson {
                            configureJackson(
                                propertyInclusion = JsonInclude.Include.USE_DEFAULTS
                            )
                        }
                    }
                })
            )
        }
    }
}