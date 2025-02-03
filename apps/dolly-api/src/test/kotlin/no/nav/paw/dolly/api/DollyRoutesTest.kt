package no.nav.paw.dolly.api

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
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

        // TODO: test resten av api og legg til oppslag mock

        "/api/v1/arbeidssoekerregistrering" - {
            "202 Accepted ved gyldig request" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    1,
                    1234
                )
                coEvery { hendelseKafkaProducerMock.sendHendelse(any(), any()) } returns Unit
                testApplication {
                    configureTestApplication(dollyService)

                    val client = configureTestClient()
                    val arbeidssoekerregistreringRequest = TestData.nyArbeidssoekerregistreringRequest()
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
                    val arbeidssoekerregistreringRequest = TestData.fullstendingArbeidssoekerregistreringRequest()
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
                        setJsonBody(TestData.nyArbeidssoekerregistreringRequest())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                }

                confirmVerified(kafkaKeysClientMock, hendelseKafkaProducerMock)
            }
        }

    }
})