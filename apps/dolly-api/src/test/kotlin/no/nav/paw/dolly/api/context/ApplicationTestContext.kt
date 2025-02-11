package no.nav.paw.dolly.api.context

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
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.dolly.api.config.APPLICATION_CONFIG
import no.nav.paw.dolly.api.config.ApplicationConfig
import no.nav.paw.dolly.api.config.SERVER_CONFIG
import no.nav.paw.dolly.api.config.ServerConfig
import no.nav.paw.dolly.api.producer.HendelseKafkaProducer
import no.nav.paw.dolly.api.client.OppslagClient
import no.nav.paw.dolly.api.plugin.installWebPlugins
import no.nav.paw.dolly.api.route.dollyRoutes
import no.nav.paw.dolly.api.service.DollyService
import no.nav.paw.dolly.api.test.createAuthProviders
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.Producer

class ApplicationTestContext {

    private val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    private val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    private val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    private val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val oppslagClientMock = mockk<OppslagClient>()
    val kafkaProducerMock = mockk<Producer<Long, Hendelse>>()
    val hendelseKafkaProducerMock = mockk<HendelseKafkaProducer>()
    val dollyServiceMock = mockk<DollyService>()
    val dollyService = DollyService(
        kafkaKeysClientMock,
        oppslagClientMock,
        hendelseKafkaProducerMock,
    )
    val mockOAuth2Server = MockOAuth2Server()
    private val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    private val healthIndicatorRepository = HealthIndicatorRepository()

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
            installWebPlugins()
            installContentNegotiationPlugin()
            installAuthenticationPlugin(applicationContext.securityConfig.authProviders)
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