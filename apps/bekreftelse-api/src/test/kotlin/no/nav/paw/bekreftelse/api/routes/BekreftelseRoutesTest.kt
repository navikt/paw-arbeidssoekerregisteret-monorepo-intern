package no.nav.paw.bekreftelse.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.paw.bekreftelse.api.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.config.AuthProvider
import no.nav.paw.bekreftelse.api.config.AuthProviders
import no.nav.paw.bekreftelse.api.config.Claims
import no.nav.paw.bekreftelse.api.plugins.configureAuthentication
import no.nav.paw.bekreftelse.api.plugins.configureHTTP
import no.nav.paw.bekreftelse.api.plugins.configureLogging
import no.nav.paw.bekreftelse.api.plugins.configureSerialization
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.bekreftelse.api.services.BekreftelseService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
private val autorisasjonServiceMock = mockk<AutorisasjonService>()
private val bekreftelseServiceMock = mockk<BekreftelseService>()
private val mockOAuth2Server = MockOAuth2Server()

class BekreftelseRoutesTest : FreeSpec({

    beforeSpec {
        clearAllMocks()
        mockOAuth2Server.start()
    }

    afterSpec {
        mockOAuth2Server.shutdown()
    }

    "Skal få 403 ved manglende Bearer Token" {
        testApplication {
            application {
                configureTestApplication()
            }

            val client = createClient {
                install(ClientContentNegotiation) {
                    jackson {}
                }
            }

            val token = mockOAuth2Server.issueToken()

            val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                bearerAuth(token.serialize())
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }
})

private fun Application.configureTestApplication() {
    configureHTTP(applicationConfig)
    configureAuthentication(applicationConfig.copy(authProviders = mockOAuth2Server.createAuthProviders()))
    configureLogging()
    configureSerialization()
    routing {
        bekreftelseRoutes(::hentKafkaKey, autorisasjonServiceMock, bekreftelseServiceMock)
    }
}

fun hentKafkaKey(ident: String): KafkaKeysResponse {
    return KafkaKeysResponse(1, 1)
}

private fun MockOAuth2Server.createAuthProviders(): AuthProviders {
    val wellKnownUrl = wellKnownUrl("default").toString()
    val tokenEndpointUrl = tokenEndpointUrl("default").toString()
    return listOf(
        AuthProvider("tokenx", wellKnownUrl, tokenEndpointUrl, "default", Claims(listOf(), true)),
        AuthProvider("azure", wellKnownUrl, tokenEndpointUrl, "default", Claims(listOf(), true))
    )
}