package no.nav.paw.bekreftelse.api.context

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.consumer.BekreftelseHttpConsumer
import no.nav.paw.bekreftelse.api.plugins.buildKafkaStreams
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.topology.buildBekreftelseTopology
import no.nav.paw.bekreftelse.api.utils.configureJackson
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.streams.KafkaStreams

data class ApplicationContext(
    val applicationConfig: ApplicationConfig,
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val bekreftelseKafkaStreams: KafkaStreams,
    val authorizationService: AuthorizationService,
    val bekreftelseService: BekreftelseService
) {
    companion object {
        fun create(applicationConfig: ApplicationConfig): ApplicationContext {
            val azureM2MTokenClient = azureAdM2MTokenClient(
                applicationConfig.runtimeEnvironment, applicationConfig.azureM2M
            )

            val kafkaKeysClient = kafkaKeysClient(applicationConfig.kafkaKeysClient) {
                azureM2MTokenClient.createMachineToMachineToken(applicationConfig.kafkaKeysClient.scope)
            }

            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            val healthIndicatorRepository = HealthIndicatorRepository()

            val httpClient = HttpClient {
                install(ContentNegotiation) {
                    jackson {
                        configureJackson()
                    }
                }
            }

            val poaoTilgangClient = PoaoTilgangCachedClient(
                PoaoTilgangHttpClient(
                    baseUrl = applicationConfig.poaoClientConfig.url,
                    { azureM2MTokenClient.createMachineToMachineToken(applicationConfig.poaoClientConfig.scope) }
                )
            )

            val authorizationService = AuthorizationService(applicationConfig, kafkaKeysClient, poaoTilgangClient)

            val bekreftelseTopology = buildBekreftelseTopology(applicationConfig, prometheusMeterRegistry)
            val bekreftelseKafkaStreams = buildKafkaStreams(
                applicationConfig,
                healthIndicatorRepository,
                bekreftelseTopology
            )

            val bekreftelseKafkaProducer = BekreftelseKafkaProducer(applicationConfig, prometheusMeterRegistry)

            val bekreftelseHttpConsumer = BekreftelseHttpConsumer(httpClient)

            val bekreftelseService = BekreftelseService(
                applicationConfig,
                bekreftelseHttpConsumer,
                bekreftelseKafkaStreams,
                bekreftelseKafkaProducer
            )

            return ApplicationContext(
                applicationConfig,
                kafkaKeysClient,
                prometheusMeterRegistry,
                healthIndicatorRepository,
                bekreftelseKafkaStreams,
                authorizationService,
                bekreftelseService
            )
        }
    }
}