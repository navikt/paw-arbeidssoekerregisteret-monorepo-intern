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
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerHandler
import no.nav.paw.bekreftelse.api.handler.KafkaProducerHandler
import no.nav.paw.bekreftelse.api.plugin.installWebPlugins
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.route.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.service.TestDataService
import no.nav.paw.bekreftelse.api.service.AuthorizationService
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.bekreftelse.api.test.createAuthProviders
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

class ApplicationTestContext {

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
    val dataSource = createTestDataSource()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val tilgangskontrollClientMock = mockk<TilgangsTjenesteForAnsatte>()
    val kafkaProducerMock = mockk<Producer<Long, Bekreftelse>>()
    val kafkaConsumerMock = mockk<KafkaConsumer<Long, BekreftelseHendelse>>()
    val kafkaProducerHandlerMock = mockk<KafkaProducerHandler>()
    val kafkaConsumerHandler = KafkaConsumerHandler(
        LivenessHealthIndicator(),
        ReadinessHealthIndicator()
    )
    val authorizationService = AuthorizationService(serverConfig, tilgangskontrollClientMock)
    val bekreftelseRepository = BekreftelseRepository()
    val bekreftelseServiceMock = mockk<BekreftelseService>()
    val bekreftelseService = BekreftelseService(
        serverConfig,
        applicationConfig,
        prometheusMeterRegistry,
        kafkaKeysClientMock,
        kafkaProducerHandlerMock,
        bekreftelseRepository
    )
    val testDataService = TestDataService(bekreftelseRepository)
    val mockOAuth2Server = MockOAuth2Server()

    fun createApplicationContext(bekreftelseService: BekreftelseService) = ApplicationContext(
        serverConfig = serverConfig,
        applicationConfig = applicationConfig,
        securityConfig = securityConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
        dataSource = dataSource,
        kafkaKeysClient = kafkaKeysClientMock,
        prometheusMeterRegistry = prometheusMeterRegistry,
        healthIndicatorRepository = HealthIndicatorRepository(),
        bekreftelseKafkaProducer = kafkaProducerMock,
        bekreftelseKafkaConsumer = kafkaConsumerMock,
        kafkaProducerHandler = kafkaProducerHandlerMock,
        kafkaConsumerHandler = kafkaConsumerHandler,
        authorizationService = authorizationService,
        bekreftelseService = bekreftelseService,
        additionalMeterBinders = emptyList()
    )

    fun ApplicationTestBuilder.configureTestApplication(bekreftelseService: BekreftelseService) {
        val applicationContext = createApplicationContext(bekreftelseService)

        application {
            installWebPlugins(applicationContext)
            installContentNegotiationPlugin()
            installErrorHandlingPlugin()
            installAuthenticationPlugin(applicationContext.securityConfig.authProviders)
            installDatabasePlugin(applicationContext.dataSource)
            routing {
                bekreftelseRoutes(applicationContext.authorizationService, applicationContext.bekreftelseService)
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

    private fun createTestDataSource(): DataSource {
        val postgres = postgresContainer()
        val databaseConfig = postgres.let {
            databaseConfig.copy(
                host = it.host,
                port = it.firstMappedPort,
                username = it.username,
                password = it.password,
                database = it.databaseName
            )
        }
        return createHikariDataSource(databaseConfig)
    }

    private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
        val postgres = PostgreSQLContainer("postgres:16").apply {
            addEnv("POSTGRES_PASSWORD", "bekreftelse_api")
            addEnv("POSTGRES_USER", "Paw1234")
            addEnv("POSTGRES_DB", "bekreftelser")
            addExposedPorts(5432)
        }
        postgres.start()
        postgres.waitingFor(Wait.forHealthcheck())
        return postgres
    }
}
