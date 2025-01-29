package no.nav.paw.dolly.api

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.Beskrivelse
import no.nav.paw.dolly.api.models.BrukerType
import no.nav.paw.dolly.api.models.Detaljer
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
                hendelseKafkaProducerMock,
                kafkaProducerMock,
                dollyServiceMock
            )
            mockOAuth2Server.shutdown()
        }

        "200 OK ved gyldig request" {
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

                response.status shouldBe HttpStatusCode.OK
                response.body<String>() shouldBe "Arbeidssøker registrert"
            }

            coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            coVerify { hendelseKafkaProducerMock.sendHendelse(any(), any()) }
        }

        "200 OK ved fullstendig request" {
            coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                2,
                1235
            )
            coEvery { hendelseKafkaProducerMock.sendHendelse(any(), any()) } returns Unit
            testApplication {
                configureTestApplication(dollyService)

                val client = configureTestClient()
                val arbeidssoekerregistreringRequest = ArbeidssoekerregistreringRequest(
                    identitetsnummer = "12345678912",
                    utfoertAv = BrukerType.SLUTTBRUKER,
                    kilde = "Dolly",
                    aarsak = "Registrering av arbeidssøker i Dolly",
                    nuskode = "3",
                    utdanningBestaatt = true,
                    utdanningGodkjent = true,
                    jobbsituasjonBeskrivelse = Beskrivelse.HAR_BLITT_SAGT_OPP,
                    jobbsituasjonDetaljer = Detaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
                    helsetilstandHindrerArbeid = false,
                    andreForholdHindrerArbeid = false
                )
                val response = client.post("/api/v1/arbeidssoekerregistrering") {
                    bearerAuth(mockOAuth2Server.issueAzureM2MToken())
                    setJsonBody(arbeidssoekerregistreringRequest)
                }

                response.status shouldBe HttpStatusCode.OK
                response.body<String>() shouldBe "Arbeidssøker registrert"
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
})