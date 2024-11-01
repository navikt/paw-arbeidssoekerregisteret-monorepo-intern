package no.nav.paw.bekreftelse.api.test

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
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerExceptionHandler
import no.nav.paw.bekreftelse.api.plugin.configTestDataPlugin
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureDatabase
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.utils.configureJackson
import no.nav.paw.bekreftelse.api.utils.createDataSource
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.config.AuthProviderClaims
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.authentication.token.AzureAd
import no.nav.paw.security.authentication.token.IdPorten
import no.nav.paw.security.authentication.token.TokenX
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

class ApplicationTestContext {

    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
    val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
    val dataSource = createTestDataSource()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val poaoTilgangClientMock = mockk<PoaoTilgangClient>()
    val kafkaProducerMock = mockk<Producer<Long, Bekreftelse>>()
    val bekreftelseKafkaProducerMock = mockk<BekreftelseKafkaProducer>()
    val kafkaConsumerMock = mockk<KafkaConsumer<Long, BekreftelseHendelse>>()
    val kafkaConsumerExceptionHandler = KafkaConsumerExceptionHandler(
        LivenessHealthIndicator(),
        ReadinessHealthIndicator()
    )
    val authorizationService = AuthorizationService(serverConfig, poaoTilgangClientMock)
    val bekreftelseRepository = BekreftelseRepository()
    val bekreftelseServiceMock = mockk<BekreftelseService>()
    val bekreftelseService = BekreftelseService(
        serverConfig,
        applicationConfig,
        prometheusMeterRegistry,
        kafkaKeysClientMock,
        bekreftelseKafkaProducerMock,
        bekreftelseRepository
    )
    val mockOAuth2Server = MockOAuth2Server()

    fun createApplicationContext(bekreftelseService: BekreftelseService) = ApplicationContext(
        serverConfig,
        applicationConfig,
        securityConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
        dataSource,
        kafkaKeysClientMock,
        prometheusMeterRegistry,
        HealthIndicatorRepository(),
        kafkaProducerMock,
        kafkaConsumerMock,
        kafkaConsumerExceptionHandler,
        authorizationService,
        bekreftelseService
    )

    fun ApplicationTestBuilder.configureTestApplication(
        bekreftelseService: BekreftelseService,
        testData: TestData = TestData()
    ) {
        val applicationContext = createApplicationContext(bekreftelseService)

        application {
            configureHTTP(applicationContext)
            configureAuthentication(applicationContext)
            configureSerialization()
            configureDatabase(applicationContext)
            configTestDataPlugin(testData)
            routing {
                bekreftelseRoutes(applicationContext)
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

    private fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
        val wellKnownUrl = wellKnownUrl("default").toString()
        return listOf(
            AuthProvider(
                name = IdPorten.name,
                clientId = "default",
                discoveryUrl = wellKnownUrl,
                claims = AuthProviderClaims(listOf("acr=idporten-loa-high"))
            ),
            AuthProvider(
                name = TokenX.name,
                clientId = "default",
                discoveryUrl = wellKnownUrl,
                claims = AuthProviderClaims(listOf("acr=Level4", "acr=idporten-loa-high"), true)
            ),
            AuthProvider(
                name = AzureAd.name,
                clientId = "default",
                discoveryUrl = wellKnownUrl,
                claims = AuthProviderClaims(listOf("NAVident"))
            )
        )
    }

    private fun createTestDataSource(): DataSource {
        val postgres = postgresContainer()
        val databaseConfig = postgres.let {
            applicationConfig.database.copy(
                jdbcUrl = "jdbc:postgresql://${it.host}:${it.firstMappedPort}/${it.databaseName}?user=${it.username}&password=${it.password}"
            )
        }
        return createDataSource(databaseConfig)
    }

    private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
        val postgres = PostgreSQLContainer("postgres:16").apply {
            addEnv("POSTGRES_PASSWORD", "bekreftelse_api")
            addEnv("POSTGRES_USER", "5up3r_53cr3t_p455w0rd")
            addEnv("POSTGRES_DB", "bekreftelser")
            addExposedPorts(5432)
        }
        postgres.start()
        postgres.waitingFor(Wait.forHealthcheck())
        return postgres
    }
}
