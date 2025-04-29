package no.nav.paw.bekreftelse.api.context

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.handler.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.bekreftelse.api.handler.KafkaProducerHandler
import no.nav.paw.bekreftelse.api.plugin.installCorsPlugins
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.route.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.service.AuthorizationService
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.bekreftelse.api.test.createAuthProviders
import no.nav.paw.bekreftelse.api.test.createTestDataSource
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import javax.sql.DataSource

data class TestContext(
    val serverConfig: ServerConfig = loadNaisOrLocalConfiguration(SERVER_CONFIG),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    val kafkaKeysClientMock: KafkaKeysClient = mockk<KafkaKeysClient>(),
    val tilgangskontrollClientMock: TilgangsTjenesteForAnsatte = mockk<TilgangsTjenesteForAnsatte>(),
    val kafkaProducerMock: Producer<Long, Bekreftelse> = mockk<Producer<Long, Bekreftelse>>(),
    val kafkaConsumerMock: KafkaConsumer<Long, BekreftelseHendelse> = mockk<KafkaConsumer<Long, BekreftelseHendelse>>(),
    val kafkaProducerHandlerMock: KafkaProducerHandler = mockk<KafkaProducerHandler>(),
    val healthIndicatorConsumerExceptionHandler: HealthIndicatorConsumerExceptionHandler = HealthIndicatorConsumerExceptionHandler(
        LivenessHealthIndicator(),
        ReadinessHealthIndicator()
    ),
    val authorizationService: AuthorizationService = AuthorizationService(serverConfig, tilgangskontrollClientMock),
    val bekreftelseRepository: BekreftelseRepository = BekreftelseRepository(),
    val bekreftelseServiceMock: BekreftelseService = mockk<BekreftelseService>(),
    val bekreftelseService: BekreftelseService = BekreftelseService(
        serverConfig,
        applicationConfig,
        prometheusMeterRegistry,
        kafkaKeysClientMock,
        kafkaProducerHandlerMock,
        bekreftelseRepository
    ),
    val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server()
) {
    private fun createApplicationContext(bekreftelseService: BekreftelseService) = ApplicationContext(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        securityConfig = securityConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
        dataSource = dataSource,
        kafkaKeysClient = kafkaKeysClientMock,
        prometheusMeterRegistry = prometheusMeterRegistry,
        healthIndicatorRepository = HealthIndicatorRepository(),
        bekreftelseKafkaProducer = kafkaProducerMock,
        bekreftelseHendelseKafkaConsumer = kafkaConsumerMock,
        kafkaProducerHandler = kafkaProducerHandlerMock,
        healthIndicatorConsumerExceptionHandler = healthIndicatorConsumerExceptionHandler,
        authorizationService = authorizationService,
        bekreftelseService = bekreftelseService,
        additionalMeterBinders = emptyList()
    )

    fun ApplicationTestBuilder.configureTestApplication(bekreftelseService: BekreftelseService) {
        with(createApplicationContext(bekreftelseService)) {

            application {
                installCorsPlugins(
                    serverConfig = serverConfig,
                    applicationConfig = applicationConfig,
                )
                installContentNegotiationPlugin()
                installErrorHandlingPlugin()
                installAuthenticationPlugin(providers = securityConfig.authProviders)
                installDatabasePlugin(dataSource = dataSource)
                routing {
                    bekreftelseRoutes(
                        authorizationService = authorizationService,
                        bekreftelseService = bekreftelseService
                    )
                }
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

    companion object {
        fun build(): TestContext {
            return TestContext(dataSource = createTestDataSource())
        }
    }
}
