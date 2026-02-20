package no.nav.paw.arbeidssoekerregisteret.route

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.arbeidssoekerregisteret.api.models.VarselResponse
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import java.util.*

class VarslerRoutesTest : FreeSpec({
    with(TestContext.buildWithH2()) {
        with(TestData) {
            beforeSpec { mockOAuth2Server.start() }
            afterSpec { mockOAuth2Server.shutdown() }
            beforeTest { initDatabase() }

            "Test suite for logikk" - {
                "Skal hente liste med varsler" {
                    val periodeId = UUID.randomUUID()
                    val varselId1 = UUID.randomUUID()
                    val varselId2 = UUID.randomUUID()
                    val varselId3 = UUID.randomUUID()
                    VarslerTable.insert(insertVarselRow(periodeId = periodeId, varselId = varselId1))
                    VarslerTable.insert(insertVarselRow(periodeId = periodeId, varselId = varselId2))
                    VarslerTable.insert(insertVarselRow(periodeId = periodeId, varselId = varselId3))

                    testApplication {
                        configureTestApplication()

                        val client = configureTestClient()
                        val token = mockOAuth2Server.issueAzureToken()
                        val response = client.get("/api/v1/varsler?periodeId=$periodeId") {
                            bearerAuth(token.serialize())
                        }

                        response.status shouldBe HttpStatusCode.OK
                        val body = response.body<List<VarselResponse>>()
                        body.size shouldBe 3
                    }
                }

                "Skal hente individuelle varsler" {
                    val periodeId = UUID.randomUUID()
                    val varselId1 = UUID.randomUUID()
                    val varselId2 = UUID.randomUUID()
                    VarslerTable.insert(insertVarselRow(periodeId = periodeId, varselId = varselId1))
                    VarslerTable.insert(insertVarselRow(periodeId = periodeId, varselId = varselId2))

                    testApplication {
                        configureTestApplication()

                        val client = configureTestClient()
                        val token = mockOAuth2Server.issueAzureToken()
                        val response1 = client.get("/api/v1/varsler/$varselId1") {
                            bearerAuth(token.serialize())
                        }
                        val response2 = client.get("/api/v1/varsler/$varselId2") {
                            bearerAuth(token.serialize())
                        }

                        response1.status shouldBe HttpStatusCode.OK
                        val body1 = response1.body<VarselResponse>()
                        body1.periodeId shouldBe periodeId
                        body1.varselId shouldBe varselId1

                        response2.status shouldBe HttpStatusCode.OK
                        val body2 = response2.body<VarselResponse>()
                        body2.periodeId shouldBe periodeId
                        body2.varselId shouldBe varselId2
                    }
                }
            }

            "Test suite for auth" - {
                "Skal få 403 Forbidden ved manglende Bearer Token" {
                    val periodeId = UUID.randomUUID()

                    testApplication {
                        configureTestApplication()

                        val client = configureTestClient()
                        val response = client.get("/api/v1/varsler?periodeId=$periodeId")

                        response.status shouldBe HttpStatusCode.Forbidden
                    }
                }

                "Skal få 403 Forbidden ved token utstedt av ukjent issuer" {
                    val periodeId = UUID.randomUUID()

                    testApplication {
                        configureTestApplication()

                        val client = configureTestClient()
                        val token = mockOAuth2Server.issueToken(
                            issuerId = "whatever",
                            claims = mapOf()
                        )
                        val response = client.get("/api/v1/varsler?periodeId=$periodeId") {
                            bearerAuth(token.serialize())
                        }

                        response.status shouldBe HttpStatusCode.Forbidden
                    }
                }

                "Skal få 403 Forbidden ved token uten noen claims" {
                    val periodeId = UUID.randomUUID()

                    testApplication {
                        configureTestApplication()

                        val client = configureTestClient()
                        val token = mockOAuth2Server.issueToken()
                        val response = client.get("/api/v1/varsler?periodeId=$periodeId") {
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
    }
})