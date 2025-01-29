package no.nav.paw.dolly.api.test

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.dolly.api.config.APPLICATION_CONFIG
import no.nav.paw.dolly.api.config.ApplicationConfig
import no.nav.paw.dolly.api.config.SERVER_CONFIG
import no.nav.paw.dolly.api.config.ServerConfig
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.dolly.api.kafka.HendelseKafkaProducer
import no.nav.paw.dolly.api.plugins.configureAuthentication
import no.nav.paw.dolly.api.plugins.configureHTTP
import no.nav.paw.dolly.api.plugins.configureSerialization
import no.nav.paw.dolly.api.routes.dollyRoutes
import no.nav.paw.dolly.api.services.DollyService
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer

class ApplicationTestContext {

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val kafkaProducerMock = mockk<Producer<Long, Hendelse>>()
    val hendelseKafkaProducerMock = mockk<HendelseKafkaProducer>()
    val dollyServiceMock = mockk<DollyService>()
    val dollyService = DollyService(
        kafkaKeysClientMock,
        hendelseKafkaProducerMock,
    )
    val mockOAuth2Server = MockOAuth2Server()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()

    fun createApplicationContext(dollyService: DollyService) = ApplicationContext(
        serverConfig,
        applicationConfig,
        securityConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
        azureAdM2MConfig,
        kafkaKeysClientMock,
        prometheusMeterRegistry,
        healthIndicatorRepository,
        kafkaProducerMock,
        dollyService
    )

    fun ApplicationTestBuilder.configureTestApplication(dollyService: DollyService) {
        val applicationContext = createApplicationContext(dollyService)

        application {
            configureAuthentication(applicationContext)
            configureHTTP()
            configureSerialization()
            routing {
                dollyRoutes(applicationContext.dollyService)
            }
        }
    }

    fun ApplicationTestBuilder.configureTestClient(): HttpClient {
        return createClient {
            install(ContentNegotiation) {
                jackson {
                    configureJackson()
                }
            }
        }
    }
}