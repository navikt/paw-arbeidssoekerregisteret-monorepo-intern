package no.nav.paw.bekreftelse.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.kafka.BekreftelseProducer
import no.nav.paw.bekreftelse.api.kafka.buildBekreftelseTopology
import no.nav.paw.bekreftelse.api.plugins.buildKafkaStreams
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.services.BekreftelseServiceImpl
import no.nav.paw.bekreftelse.api.services.BekreftelseServiceMock
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.streams.KafkaStreams

fun createDependencies(applicationConfig: ApplicationConfig): Dependencies {
    val azureM2MTokenClient = azureAdM2MTokenClient(applicationConfig.naisEnv, applicationConfig.azureM2M)

    val kafkaKeysClient = kafkaKeysClient(applicationConfig.kafkaKeysClient) {
        azureM2MTokenClient.createMachineToMachineToken(applicationConfig.kafkaKeysClient.scope)
    }

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val healthIndicatorRepository = HealthIndicatorRepository()

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val poaoTilgangClient = PoaoTilgangCachedClient(
        PoaoTilgangHttpClient(
            applicationConfig.poaoClientConfig.url,
            { azureM2MTokenClient.createMachineToMachineToken(applicationConfig.poaoClientConfig.scope) }
        )
    )

    val autorisasjonService = AutorisasjonService(poaoTilgangClient)

    val bekreftelseTopology = buildBekreftelseTopology(applicationConfig, prometheusMeterRegistry)
    val bekreftelseKafkaStreams = buildKafkaStreams(applicationConfig, healthIndicatorRepository, bekreftelseTopology)

    // TODO Bruker mock for utvikling
    val bekreftelseService: BekreftelseService = if (applicationConfig.brukMock) {
        healthIndicatorRepository.getLivenessIndicators().forEach { it.setHealthy() }
        healthIndicatorRepository.getReadinessIndicators().forEach { it.setHealthy() }

        BekreftelseServiceMock()
    } else {
        val bekreftelseProducer = BekreftelseProducer(applicationConfig)

        BekreftelseServiceImpl(
            applicationConfig,
            httpClient,
            bekreftelseKafkaStreams,
            bekreftelseProducer
        )
    }

    return Dependencies(
        kafkaKeysClient,
        prometheusMeterRegistry,
        healthIndicatorRepository,
        bekreftelseKafkaStreams,
        autorisasjonService,
        bekreftelseService
    )
}

data class Dependencies(
    val kafkaKeysClient: KafkaKeysClient,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val bekreftelseKafkaStreams: KafkaStreams,
    val autorisasjonService: AutorisasjonService,
    val bekreftelseService: BekreftelseService
)