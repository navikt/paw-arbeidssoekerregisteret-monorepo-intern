package no.nav.paw.arbeidssokerregisteret.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssokerregisteret.TestData
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.security.mock.oauth2.MockOAuth2Server

class TokenxAuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val testAuthUrl = "/testAuthTokenx"

    beforeSpec {
        oauth.start()
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
                        "pid" to TestData.foedselsnummer.value
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
                        "pid" to TestData.foedselsnummer.value
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
                        "pid" to TestData.foedselsnummer.value
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
                        "pid" to TestData.foedselsnummer.value
                    ),
                    expiry = 0
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Request with malformed token should return 401") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = "malformed-token"
                val response = client.get(testAuthUrl) { bearerAuth(token) }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Authenticated request should return 200") {
            testApplication {
                application { testAuthModule(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to TestData.foedselsnummer.value
                    )
                )
                val response = client.get(testAuthUrl) { bearerAuth(token.serialize()) }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("For a authenticated TokenX request we should be able to resolve the pid") {
            testApplication {
                application { configureAuthentication(oauth) }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to TestData.foedselsnummer.value
                    )
                )
                routing {
                    authenticate("tokenx") {
                        get("/test") {
                            val scope = requestScope()
                            scope.shouldNotBeNull()
                            scope.claims[TokenXPID]?.value shouldBe TestData.foedselsnummer.value
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                val response = client.get("/test") { bearerAuth(token.serialize()) }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
