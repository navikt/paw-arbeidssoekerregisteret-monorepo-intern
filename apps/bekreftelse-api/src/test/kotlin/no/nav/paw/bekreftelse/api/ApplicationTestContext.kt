package no.nav.paw.bekreftelse.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AuthProvider
import no.nav.paw.bekreftelse.api.config.AuthProviderClaims
import no.nav.paw.bekreftelse.api.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.context.resolveRequest
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerExceptionHandler
import no.nav.paw.bekreftelse.api.model.Azure
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.model.TokenX
import no.nav.paw.bekreftelse.api.plugin.TestData
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
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

class ApplicationTestContext {

    val testData = TestDataGenerator()
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
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
    val authorizationService = AuthorizationService(
        serverConfig,
        applicationConfig,
        kafkaKeysClientMock,
        poaoTilgangClientMock
    )
    val bekreftelseRepository = BekreftelseRepository()
    val bekreftelseServiceMock = mockk<BekreftelseService>()
    val bekreftelseService = BekreftelseService(
        serverConfig,
        applicationConfig,
        prometheusMeterRegistry,
        bekreftelseKafkaProducerMock,
        bekreftelseRepository
    )
    val mockOAuth2Server = MockOAuth2Server()

    fun createApplicationContext(bekreftelseService: BekreftelseService) = ApplicationContext(
        serverConfig,
        applicationConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
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

    fun ApplicationTestBuilder.configureSimpleTestApplication(bekreftelseService: BekreftelseService) {
        val applicationContext = createApplicationContext(bekreftelseService)

        application {
            configureHTTP(applicationContext)
            configureAuthentication(applicationContext)
            configureSerialization()
            routing {
                testRoutes()
            }
        }
    }

    fun ApplicationTestBuilder.configureCompleteTestApplication(
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

    private fun Route.testRoutes() {
        route("/api/secured") {
            authenticate(TokenX.name, Azure.name) {
                get("/") {
                    authorizationService.authorize(resolveRequest(), TilgangType.LESE)
                    call.respond("WHATEVER")
                }

                post<TilgjengeligeBekreftelserRequest>("/") { request ->
                    authorizationService.authorize(
                        requestContext = resolveRequest(request.identitetsnummer),
                        tilgangType = TilgangType.SKRIVE
                    )
                    call.respond("WHATEVER")
                }
            }
        }
    }

    private fun MockOAuth2Server.createAuthProviders(): List<AuthProvider> {
        val wellKnownUrl = wellKnownUrl("default").toString()
        return listOf(
            AuthProvider(
                TokenX.name, wellKnownUrl, "default", AuthProviderClaims(
                    listOf(
                        "acr=Level4", "acr=idporten-loa-high"
                    ), true
                )
            ),
            AuthProvider(
                Azure.name, wellKnownUrl, "default", AuthProviderClaims(
                    listOf(
                        "NAVident"
                    )
                )
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
