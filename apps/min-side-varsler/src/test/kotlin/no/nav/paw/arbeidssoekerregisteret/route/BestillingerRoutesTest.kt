package no.nav.paw.arbeidssoekerregisteret.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingResponse
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import java.util.*

class BestillingerRoutesTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        beforeSpec { mockOAuth2Server.start() }
        afterSpec { mockOAuth2Server.shutdown() }
        beforeTest { initDatabase() }

        "Test suite for logikk" - {
            "Skal opprette og bekrefte bestilling" {
                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueAzureToken()
                    val response1 = client.post("/api/v1/bestillinger") {
                        bearerAuth(token.serialize())
                    }
                    response1.status shouldBe HttpStatusCode.OK
                    val body1 = response1.body<BestillingResponse>()
                    body1.status shouldBe BestillingStatus.VENTER

                    val response2 = client.get("/api/v1/bestillinger/${body1.bestillingId}") {
                        bearerAuth(token.serialize())
                    }
                    val body2 = response2.body<BestillingResponse>()
                    body2.bestillingId shouldBe body1.bestillingId
                    body2.status shouldBe BestillingStatus.VENTER

                    val response3 = client.put("/api/v1/bestillinger/${body1.bestillingId}") {
                        bearerAuth(token.serialize())
                    }
                    val body3 = response3.body<BestillingResponse>()
                    body3.bestillingId shouldBe body1.bestillingId
                    body3.status shouldBe BestillingStatus.BEKREFTET

                    val response4 = client.get("/api/v1/bestillinger/${body1.bestillingId}") {
                        bearerAuth(token.serialize())
                    }
                    val body4 = response4.body<BestillingResponse>()
                    body4.bestillingId shouldBe body1.bestillingId
                    body4.status shouldBe BestillingStatus.BEKREFTET
                }
            }
        }

        "Test suite for auth" - {
            "Skal få 403 Forbidden ved manglende Bearer Token" {
                val bestillingId = UUID.randomUUID()

                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()
                    val response = client.get("/api/v1/bestillinger/$bestillingId")

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 Forbidden ved token utstedt av ukjent issuer" {
                val bestillingId = UUID.randomUUID()

                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueToken(
                        issuerId = "whatever",
                        claims = mapOf()
                    )
                    val response = client.get("/api/v1/bestillinger/$bestillingId") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 Forbidden ved token uten noen claims" {
                val bestillingId = UUID.randomUUID()

                testApplication {
                    configureTestApplication()

                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueToken()
                    val response = client.get("/api/v1/bestillinger/$bestillingId") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe UgyldigBearerTokenException("").type
                }
            }
        }
    }
})