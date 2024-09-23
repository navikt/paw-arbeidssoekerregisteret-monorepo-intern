package no.nav.paw.bekreftelse.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AuthProvider
import no.nav.paw.bekreftelse.api.config.AuthProviders
import no.nav.paw.bekreftelse.api.config.Claims
import no.nav.paw.bekreftelse.api.consumer.BekreftelseHttpConsumer
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.producer.BekreftelseKafkaProducer
import no.nav.paw.bekreftelse.api.routes.bekreftelseRoutes
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.bekreftelse.api.utils.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore

class ApplicationTestContext {

    val testData = TestDataGenerator()
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val kafkaStreamsMock = mockk<KafkaStreams>()
    val stateStoreMock = mockk<ReadOnlyKeyValueStore<Long, InternState>>()
    val bekreftelseKafkaProducerMock = mockk<BekreftelseKafkaProducer>()
    val bekreftelseHttpConsumerMock = mockk<BekreftelseHttpConsumer>()
    val autorisasjonServiceMock = mockk<AutorisasjonService>()
    val bekreftelseServiceMock = mockk<BekreftelseService>()
    val bekreftelseServiceReal = BekreftelseService(
        applicationConfig,
        bekreftelseHttpConsumerMock,
        kafkaStreamsMock,
        bekreftelseKafkaProducerMock
    )
    val mockOAuth2Server = MockOAuth2Server()

    fun kafkaKeysFunction(ident: String): KafkaKeysResponse {
        return KafkaKeysResponse(1, 1)
    }

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
        application {
            configureHTTP(applicationConfig)
            configureAuthentication(applicationConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()))
            configureLogging()
            configureSerialization()
            routing {
                bekreftelseRoutes(::kafkaKeysFunction, autorisasjonServiceMock, bekreftelseService)
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
