package no.nav.paw.bekreftelse.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.config.APPLICATION_CONFIG
import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.SERVER_CONFIG
import no.nav.paw.bekreftelse.config.ServerConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val healthIndicatorRepository = HealthIndicatorRepository()

            return ApplicationContext(
                serverConfig,
                applicationConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository
            )
        }
    }
}