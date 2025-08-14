package no.nav.paw.security.texas

import TexasClient
import TokenExchangeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.paw.serialization.jackson.configureJackson

class TexasClientTest : FreeSpec({
    val texasTestEndpoint = "https://texas/token"

    "Kan veksle token for bruker" {
        val expectedToken = "vekslet_token_bruker"
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe texasTestEndpoint

            respond(
                content = """{ "access_token": "$expectedToken" }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }

        val texasClient = TexasClient(
            config = TexasClientConfig(
                endpoint = texasTestEndpoint,
                target = "target-app"
            ),
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            val response = texasClient.exchangeOnBehalfOfBrukerToken(
                OnBehalfOfBrukerRequest(userToken = "user-token", target = "target-app")
            )
            response.accessToken shouldBe "vekslet_token_bruker"
        }
    }

    "Kan veksle token for ansatt" {
        val expectedToken = "vekslet_token_ansatt"
        val target = "target-app"
        val mockEngine = MockEngine { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe texasTestEndpoint

            respond(
                content = """{ "access_token": "$expectedToken" }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }

        val texasClient = TexasClient(
            config = TexasClientConfig(endpoint = texasTestEndpoint, target),
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            val response = texasClient.exchangeOnBehalfOfAnsattToken(
                OnBehalfOfAnsattRequest(userToken = "user-token", target)
            )
            response.accessToken shouldBe expectedToken
        }
    }

    "Kaster TokenExchangeException ved ikke-200 statuskode" {
        val expectedStatusCode = HttpStatusCode.BadRequest
        val target = "target-app"
        val mockEngine = MockEngine {
            respond(
                content = """{ "error": "no token for you" }""",
                status = expectedStatusCode,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }

        val texasClient = TexasClient(
            config = TexasClientConfig(endpoint = texasTestEndpoint, target),
            httpClient = testClient(mockEngine)
        )

        runBlocking {
            val tokenExchangeException = shouldThrow<TokenExchangeException> {
                texasClient.exchangeOnBehalfOfBrukerToken(
                    OnBehalfOfBrukerRequest(userToken = "user-token", target)
                )
            }
            tokenExchangeException.message shouldContain "Statuskode: ${expectedStatusCode.value}"
        }
    }
})

private fun testClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
