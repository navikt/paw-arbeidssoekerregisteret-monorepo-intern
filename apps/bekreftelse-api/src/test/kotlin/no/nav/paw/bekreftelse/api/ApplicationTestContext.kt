package no.nav.paw.bekreftelse.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.bekreftelse.api.authz.authorize
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AuthProvider
import no.nav.paw.bekreftelse.api.config.AuthProviders
import no.nav.paw.bekreftelse.api.config.Claims
import no.nav.paw.bekreftelse.api.consumer.BekreftelseHttpConsumer
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.utils.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

class ApplicationTestContext {

    val testData = TestDataGenerator()
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val prometheusMeterRegistryMock = mockk<PrometheusMeterRegistry>()
    val kafkaStreamsMock = mockk<KafkaStreams>()
    val stateStoreMock = mockk<ReadOnlyKeyValueStore<Long, InternState>>()
    val kafkaKeysClientMock = mockk<KafkaKeysClient>()
    val poaoTilgangClientMock = mockk<PoaoTilgangClient>()
    val bekreftelseKafkaProducerMock = mockk<BekreftelseKafkaProducer>()
    val bekreftelseHttpConsumerMock = mockk<BekreftelseHttpConsumer>()
    val authorizationService = AuthorizationService(kafkaKeysClientMock, poaoTilgangClientMock)
    val bekreftelseServiceMock = mockk<BekreftelseService>()
    val bekreftelseServiceReal = BekreftelseService(
        applicationConfig,
        bekreftelseHttpConsumerMock,
        kafkaStreamsMock,
        bekreftelseKafkaProducerMock
    )
    val mockOAuth2Server = MockOAuth2Server()

    fun MockOAuth2Server.createAuthProviders(): AuthProviders {
        val wellKnownUrl = wellKnownUrl("default").toString()
        return listOf(
            AuthProvider(
                "idporten", wellKnownUrl, "default", Claims(
                    listOf(
                        "acr=idporten-loa-high"
                    )
                )
            ),
            AuthProvider(
                "tokenx", wellKnownUrl, "default", Claims(
                    listOf(
                        "acr=Level4", "acr=idporten-loa-high"
                    ), true
                )
            ),
            AuthProvider(
                "azure", wellKnownUrl, "default", Claims(
                    listOf(
                        "NAVident"
                    )
                )
            )
        )
    }

    fun ApplicationTestBuilder.configureTestApplication(bekreftelseService: BekreftelseService) {
        val applicationContext = ApplicationContext(
            applicationConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()),
            kafkaKeysClientMock,
            prometheusMeterRegistryMock,
            HealthIndicatorRepository(),
            kafkaStreamsMock,
            authorizationService,
            bekreftelseService
        )

        application {
            configureHTTP(applicationContext)
            configureAuthentication(applicationContext)
            configureLogging()
            configureSerialization()
            routing {
                bekreftelseRoutes(applicationContext)
                route("/api/secured") {
                    authenticate("idporten", "tokenx", "azure") {
                        get("/") {
                            with(authorize(null, authorizationService, TilgangType.LESE)) {
                                call.respond("WHATEVER")
                            }
                        }

                        post<TilgjengeligeBekreftelserRequest>("/") { request ->
                            with(authorize(request.identitetsnummer, authorizationService, TilgangType.SKRIVE)) {
                                call.respond("WHATEVER")
                            }
                        }
                    }
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
}