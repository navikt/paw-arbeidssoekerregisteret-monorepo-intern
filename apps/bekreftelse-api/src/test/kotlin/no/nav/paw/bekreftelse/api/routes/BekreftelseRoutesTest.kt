package no.nav.paw.bekreftelse.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.paw.bekreftelse.api.models.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.models.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.test.ApplicationTestContext
import no.nav.paw.bekreftelse.api.test.TestData
import no.nav.paw.bekreftelse.api.test.issueAzureToken
import no.nav.paw.bekreftelse.api.test.issueTokenXToken
import no.nav.paw.bekreftelse.api.test.setJsonBody
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException


class BekreftelseRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {
        beforeSpec {
            clearAllMocks()
            mockOAuth2Server.start()
        }

        afterSpec {
            confirmVerified(
                kafkaKeysClientMock,
                tilgangskontrollClientMock,
                bekreftelseKafkaProducerMock,
                kafkaProducerMock,
                kafkaConsumerMock,
                bekreftelseServiceMock
            )
            mockOAuth2Server.shutdown()
        }

        /*
         * FELLES TESTER
         */
        "Test suite for felleslogikk" - {
            "Skal få 403 Forbidden ved manglende Bearer Token" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser")

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 Forbidden ved token utstedt av ukjent issuer" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueToken(
                        issuerId = "whatever",
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to TestData.fnr1
                        )
                    )
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "Skal få 403 Forbidden ved token uten noen claims" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueToken().serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe UgyldigBearerTokenException("").type
                }
            }
        }

        /*
         * SLUTTBRUKER TESTER
         */
        "Test suite for sluttbruker" - {
            "Skal få 403 Forbidden ved TokenX-token uten pid claim" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high"
                        )
                    )
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe UgyldigBearerTokenException("").type
                }
            }

            "Skal 403 Forbidden ved hente tilgjengelige bekreftelser med POST-request når innsendt fnr ikke er samme som token pid" {
                val request = TestData.nyTilgjengeligeBekreftelserRequest(identitetsnummer = TestData.fnr2)

                testApplication {
                    configureTestApplication(bekreftelseService)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr1))
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }
            }

            "Skal hente tilgjengelige bekreftelser med GET-request" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId1,
                    TestData.kafkaKey1
                )
                val bereftelseRows = TestData.nyBekreftelseRows(
                    arbeidssoekerId = TestData.arbeidssoekerId1,
                    periodeId = TestData.periodeId1,
                    bekreftelseRow = listOf(
                        TestData.kafkaOffset1 to TestData.bekreftelseId1,
                        TestData.kafkaOffset2 to TestData.bekreftelseId2
                    )
                )

                testApplication {
                    configureTestApplication(bekreftelseService)

                    testDataService.opprettBekreftelser(bereftelseRows)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr1))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 2

                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            }

            "Skal hente tilgjengelige bekreftelser med POST-request" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId2,
                    TestData.kafkaKey2
                )
                val bereftelseRows = TestData.nyBekreftelseRows(
                    arbeidssoekerId = TestData.arbeidssoekerId2,
                    periodeId = TestData.periodeId2,
                    bekreftelseRow = listOf(
                        TestData.kafkaOffset3 to TestData.bekreftelseId3,
                        TestData.kafkaOffset4 to TestData.bekreftelseId4
                    )
                )
                val request = TestData.nyTilgjengeligeBekreftelserRequest(identitetsnummer = TestData.fnr2)

                testApplication {
                    configureTestApplication(bekreftelseService)

                    testDataService.opprettBekreftelser(bereftelseRows)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr2))
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 2

                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            }

            "Skal få 403 Forbidden ved motta bekreftelse når ident og pid er ulik" {
                val request = TestData.nyBekreftelseRequest(
                    identitetsnummer = TestData.fnr4,
                    bekreftelseId = TestData.bekreftelseId5
                )

                testApplication {
                    configureTestApplication(bekreftelseService)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }
            }

            "Skal motta bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId3,
                    TestData.kafkaKey3
                )
                every { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) } just runs
                val bereftelseRows = TestData.nyBekreftelseRows(
                    arbeidssoekerId = TestData.arbeidssoekerId3,
                    periodeId = TestData.periodeId3,
                    bekreftelseRow = listOf(
                        TestData.kafkaOffset5 to TestData.bekreftelseId5
                    )
                )
                val request = TestData.nyBekreftelseRequest(
                    identitetsnummer = TestData.fnr3,
                    bekreftelseId = TestData.bekreftelseId5
                )

                testApplication {
                    configureTestApplication(bekreftelseService)

                    testDataService.opprettBekreftelser(bereftelseRows)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr3))
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.OK
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                verify { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) }
            }

            "Skal motta bekreftelse men finner ikke relatert tilgjengelig bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId4,
                    TestData.kafkaKey4
                )
                val request = TestData.nyBekreftelseRequest(identitetsnummer = TestData.fnr4)

                testApplication {
                    configureTestApplication(bekreftelseService)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(mockOAuth2Server.issueTokenXToken(pid = TestData.fnr4))
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            }
        }

        /*
         * VEILEDER TESTER
         */
        "Test suite for veildere" - {

            "Skal få 403 Forbidden ved GET-request" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }
            }

            "Skal få 403 Forbidden ved POST-request uten ident" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        setJsonBody(TilgjengeligeBekreftelserRequest())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }
            }

            "Skal få 403 Forbidden ved POST-request men uten POAO tilgang" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(1, 1)
                coEvery {
                    tilgangskontrollClientMock.harAnsattTilgangTilPerson(
                        any(),
                        any(),
                        any()
                    )
                } returns Data(false)

                testApplication {
                    configureTestApplication(bekreftelseServiceMock)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        setJsonBody(TilgjengeligeBekreftelserRequest("01017012345"))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.type shouldBe IngenTilgangException("").type
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }

            "Skal hente tilgjengelige bekreftelser" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId5,
                    TestData.kafkaKey5
                )
                coEvery { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) } returns Data(true)
                val bereftelseRows = TestData.nyBekreftelseRows(
                    arbeidssoekerId = TestData.arbeidssoekerId5,
                    periodeId = TestData.periodeId4,
                    bekreftelseRow = listOf(
                        TestData.kafkaOffset6 to TestData.bekreftelseId6,
                        TestData.kafkaOffset7 to TestData.bekreftelseId7
                    )
                )
                val request = TestData.nyTilgjengeligeBekreftelserRequest(identitetsnummer = TestData.fnr4)

                testApplication {
                    configureTestApplication(bekreftelseService)

                    testDataService.opprettBekreftelser(bereftelseRows)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 2
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
            }

            "Skal motta bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    TestData.arbeidssoekerId6,
                    TestData.kafkaKey6
                )
                coEvery { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) } returns Data(true)
                every { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) } just runs
                val bereftelseRows = TestData.nyBekreftelseRows(
                    arbeidssoekerId = TestData.arbeidssoekerId6,
                    periodeId = TestData.periodeId5,
                    bekreftelseRow = listOf(
                        TestData.kafkaOffset8 to TestData.bekreftelseId8
                    )
                )
                val request = TestData.nyBekreftelseRequest(
                    identitetsnummer = TestData.fnr5,
                    bekreftelseId = TestData.bekreftelseId8
                )

                testApplication {
                    configureTestApplication(bekreftelseService)

                    testDataService.opprettBekreftelser(bereftelseRows)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(mockOAuth2Server.issueAzureToken())
                        setJsonBody(request)
                    }

                    response.status shouldBe HttpStatusCode.OK
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { tilgangskontrollClientMock.harAnsattTilgangTilPerson(any(), any(), any()) }
                verify { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) }
            }
        }
    }
})