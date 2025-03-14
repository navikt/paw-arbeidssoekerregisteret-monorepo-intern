package no.nav.paw.bekreftelsetjeneste.context

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.OddetallPartallMap
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient

class ApplicationContext(
    val bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    val applicationConfig: ApplicationConfig,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val kafkaKeysClient: KafkaKeysClient,
    val oddetallPartallMap: OddetallPartallMap
) {
    val bekreftelseHendelseSerde = BekreftelseHendelseSerde()

    companion object {
        fun create(
            applicationConfig: ApplicationConfig,
            bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
            oddetallPartallMap: OddetallPartallMap
        ): ApplicationContext {
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val healthIndicatorRepository = HealthIndicatorRepository()

            val azureM2MTokenClient = azureAdM2MTokenClient(
                applicationConfig.runtimeEnvironment, applicationConfig.azureM2M
            )

            val kafkaKeysClient = kafkaKeysClient(applicationConfig.kafkaKeysClient) {
                azureM2MTokenClient.createMachineToMachineToken(applicationConfig.kafkaKeysClient.scope)
            }

            return ApplicationContext(
                bekreftelseKonfigurasjon,
                applicationConfig,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                kafkaKeysClient,
                oddetallPartallMap
            )
        }
    }
}