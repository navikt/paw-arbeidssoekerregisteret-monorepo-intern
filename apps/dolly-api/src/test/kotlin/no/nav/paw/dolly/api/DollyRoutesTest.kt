package no.nav.paw.dolly.api

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import no.nav.paw.dolly.api.models.TypeRequest
import no.nav.paw.dolly.api.test.ApplicationTestContext
import no.nav.paw.dolly.api.test.TestData
import no.nav.paw.dolly.api.test.issueAzureM2MToken
import no.nav.paw.dolly.api.test.setJsonBody
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse

class DollyRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {
        beforeSpec {
            clearAllMocks()
            mockOAuth2Server.start()
        }

        afterSpec {
            confirmVerified(
                kafkaKeysClientMock,
                oppslagClientMock,
                hendelseKafkaProducerMock,
                kafkaProducerMock,
                dollyServiceMock
            )
            mockOAuth2Server.shutdown()
        }

        val identitetsnummer = "12345678911"

        "POST /api/v1/arbeidssoekerregistrering" - {
            "202 Accepted ved gyldig request" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    1,
                    1234
                )
                coEvery { hendelseKafkaProducerMock.sendHendelse(any(), any()) } returns Unit
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val arbeidssoekerregistreringRequest = TestData.nyArbeidssoekerregistreringRequest(identitetsnummer)
                    val response = client.post("/api/v1/arbeidssoekerregistrering") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                        setJsonBody(arbeidssoekerregistreringRequest)
                    }

                    response.status shouldBe HttpStatusCode.Accepted
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { hendelseKafkaProducerMock.sendHendelse(any(), any()) }
            }

            "202 Accepted ved fullstendig request" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    2,
                    1235
                )
                coEvery { hendelseKafkaProducerMock.sendHendelse(any(), any()) } returns Unit
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val arbeidssoekerregistreringRequest = TestData.fullstendingArbeidssoekerregistreringRequest(identitetsnummer)
                    val response = client.post("/api/v1/arbeidssoekerregistrering") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                        setJsonBody(arbeidssoekerregistreringRequest)
                    }

                    response.status shouldBe HttpStatusCode.Accepted
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { hendelseKafkaProducerMock.sendHendelse(any(), any()) }
            }

            "400 Bad Request ved ugyldig request" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoekerregistrering") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                        setJsonBody("ugyldig request")
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }

            "403 Forbidden ved manglende token" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoekerregistrering") {
                        setJsonBody(TestData.nyArbeidssoekerregistreringRequest(identitetsnummer))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }
        }

        "DELETE /api/v1/arbeidssoekerregistrering/{identitetsnummer}" - {
            "204 No Content" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    1,
                    1234
                )
                coEvery { hendelseKafkaProducerMock.sendHendelse(any(), any()) } returns Unit
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.delete("/api/v1/arbeidssoekerregistrering/$identitetsnummer") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.NoContent
                }

                coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                coVerify { hendelseKafkaProducerMock.sendHendelse(any(), any()) }
            }

            "400 Bad Request ved ugyldig request" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.delete("/api/v1/arbeidssoekerregistrering/1234") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }

            "403 Forbidden ved manglende token" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.delete("/api/v1/arbeidssoekerregistrering/$identitetsnummer") {
                        setJsonBody(TestData.nyArbeidssoekerregistreringRequest(identitetsnummer))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }
        }

        "GET /api/v1/arbeidssoekerregistrering/{identitetsnummer}" - {
            "200 OK" {
                coEvery { oppslagClientMock.hentAggregerteArbeidssoekerperioder(any()) } returns TestData.oppslagsApiResponse()
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/arbeidssoekerregistrering/$identitetsnummer") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.OK
                }

                coVerify { oppslagClientMock.hentAggregerteArbeidssoekerperioder(any<String>()) }
            }

            "400 Bad Request ved ugyldig request" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/arbeidssoekerregistrering/1234") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }

            "403 Forbidden ved manglende token" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/arbeidssoekerregistrering/$identitetsnummer")

                    response.status shouldBe HttpStatusCode.Forbidden
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }
        }

        "GET /api/v1/typer/{type}" - {
            "200 OK" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/typer/${TypeRequest.JOBBSITUASJONSBESKRIVELSE}") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.OK
                }
            }

            "400 Bad Request ved ugyldig request" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/typer/test") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }

            "403 Forbidden ved manglende token" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/typer/${TypeRequest.JOBBSITUASJONSBESKRIVELSE}")

                    response.status shouldBe HttpStatusCode.Forbidden
                }
            }

            "404 Not Found ved ugyldig type" {
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val response = client.get("/api/v1/typer/") {
                        bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    }

                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }

    }
})