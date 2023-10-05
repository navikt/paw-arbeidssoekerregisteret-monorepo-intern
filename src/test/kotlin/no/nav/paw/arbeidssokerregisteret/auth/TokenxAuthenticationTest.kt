package no.nav.paw.arbeidssokerregisteret.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssokerregisteret.TestData
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.loadConfiguration
import no.nav.paw.arbeidssokerregisteret.plugins.configureAuthentication
import no.nav.security.mock.oauth2.MockOAuth2Server

class AuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthTokenx"

    beforeSpec {
        oauth.start(8081)
    }

    afterSpec {
        oauth.shutdown()
    }

    context("protected tokenx endpoints") {
        test("Unauthenticated request should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val response = client.get(testAuthUrl)

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with wrong issuer should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to TestData.foedselsnummer.verdi
                    ),
                    issuerId = "wrong-issuer"
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with wrong audience should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to TestData.foedselsnummer.verdi
                    ),
                    audience = "wrong-audience"
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with too low acr claim should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-low",
                        "pid" to TestData.foedselsnummer.verdi
                    )
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request without pid claim should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-low"
                    )
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request without expired token should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-low",
                        "pid" to TestData.foedselsnummer.verdi
                    ),
                    expiry = 0
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Authenticated request should return 200") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to TestData.foedselsnummer.verdi
                    )
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})

fun Application.testAuthModule(oAuth2Server: MockOAuth2Server) {
    val config = loadConfiguration<Config>()
    val authProviders = config.authProviders.copy(
        tokenx = config.authProviders.tokenx.copy(
            discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
            clientId = "default"
        ),
        azure = config.authProviders.azure.copy(
            discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
            clientId = "default"
        )
    )
    configureAuthentication(authProviders)
    routing {
        authenticate("tokenx") {
            route("/testAuthTokenx") {
                get {
                    call.respond(HttpStatusCode.OK, "Hello World!")
                }
            }
        }
        authenticate("azure") {
            route("/testAuthAzure") {
                get {
                    call.respond(HttpStatusCode.OK, "Hello World!")
                }
            }
        }
    }
}
