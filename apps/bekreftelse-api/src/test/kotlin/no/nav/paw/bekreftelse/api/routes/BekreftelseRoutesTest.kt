package no.nav.paw.bekreftelse.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.paw.bekreftelse.api.ApplicationTestContext
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.plugin.TestData
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse

class BekreftelseRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {
        beforeSpec {
            clearAllMocks()
            mockOAuth2Server.start()
        }

        afterSpec {
            coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            confirmVerified(
                kafkaKeysClientMock,
                poaoTilgangClientMock,
                bekreftelseKafkaProducerMock,
                kafkaProducerMock,
                kafkaConsumerMock,
                bekreftelseServiceMock
            )
            mockOAuth2Server.shutdown()
        }

        /*
        * SLUTTBRUKER TESTER
        */
        "Test suite for sluttbruker" - {
            "Skal hente tilgjengelige bekreftelser" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidssoekerId1,
                    testData.kafkaKey1
                )

                testApplication {
                    configureCompleteTestApplication(
                        bekreftelseService, TestData(
                            bereftelseRows = testData.nyBekreftelseRows(
                                arbeidssoekerId = testData.arbeidssoekerId1,
                                periodeId = testData.periodeId1,
                                bekreftelseRow = listOf(
                                    1L to testData.bekreftelseId1, 2L to testData.bekreftelseId2
                                )
                            )
                        )
                    )
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to testData.fnr1
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 2
                }
            }

            "Skal motta bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidssoekerId2,
                    testData.kafkaKey2
                )
                every { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) } just runs
                val request = testData.nyBekreftelseRequest(
                    identitetsnummer = testData.fnr2,
                    bekreftelseId = testData.bekreftelseId3
                )

                testApplication {
                    configureCompleteTestApplication(
                        bekreftelseService, TestData(
                            bereftelseRows = testData.nyBekreftelseRows(
                                arbeidssoekerId = testData.arbeidssoekerId2,
                                periodeId = testData.periodeId2,
                                bekreftelseRow = listOf(
                                    3L to testData.bekreftelseId3
                                )
                            )
                        )
                    )
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to testData.fnr2
                        )
                    )

                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(request)
                    }

                    response.status shouldBe HttpStatusCode.OK
                    verify { bekreftelseKafkaProducerMock.produceMessage(any<Long>(), any<Bekreftelse>()) }
                }
            }

            "Skal motta bekreftelse men finner ikke relatert tilgjengelig bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidssoekerId3,
                    testData.kafkaKey3
                )
                val request = testData.nyBekreftelseRequest(identitetsnummer = testData.fnr3)

                testApplication {
                    configureCompleteTestApplication(bekreftelseService)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to testData.fnr3
                        )
                    )

                    val response = client.post("/api/v1/bekreftelse") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(request)
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }
})