package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import arrow.core.right
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.backup.utils.TestApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerRequest
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.BrukerType
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.backup.utils.avvist
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestApplication
import no.nav.paw.arbeidssoekerregisteret.backup.utils.configureTestClient
import no.nav.paw.arbeidssoekerregisteret.backup.utils.opplysninger
import no.nav.paw.arbeidssoekerregisteret.backup.utils.startet
import no.nav.paw.arbeidssoekerregisteret.backup.utils.storedHendelseRecord
import no.nav.paw.arbeidssoekerregisteret.backup.utils.toApplicationContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse

class BrukerstoetteApiTest : FreeSpec({
    val testApplicationContext = TestApplicationContext.build()
    afterEach { clearAllMocks() }

    with(testApplicationContext) {
        "Test av brukerstøtte API" - {
            "Ingen hendelser funnet gir 404 Not Found" {

                coEvery {
                    kafkaKeysClient.getIdAndKeyOrNull("12345678901")
                } returns KafkaKeysResponse(id = 1, key = 1L)

                every {
                    hendelseRecordRepository.readAllNestedRecordsForId(any(), any(), any(), any())
                } returns emptyList()

                testApplication {
                    configureTestApplication(testApplicationContext.toApplicationContext())
                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(DetaljerRequest("12345678901"))
                    }

                    response.status shouldBe HttpStatusCode.NotFound
                    val responseBody = response.body<no.nav.paw.error.model.ProblemDetails>()
                    responseBody.detail shouldBe "Fant ingen hendelser for person"
                    responseBody.status shouldBe HttpStatusCode.NotFound
                }

            }

            "Ugyldig identformat resulterer i 400 bad request" {
                coEvery { brukerstoetteService.hentDetaljer(any()) } throws UgyldigIdentFormat("Ugyldig identformat")

                testApplication {
                    configureTestApplication(testApplicationContext.toApplicationContext())
                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(DetaljerRequest("12345678901"))
                    }

                    response.status shouldBe HttpStatusCode.BadRequest
                    val responseBody = response.body<no.nav.paw.error.model.ProblemDetails>()
                    responseBody.detail shouldBe "Ugyldig identformat"
                    responseBody.status shouldBe HttpStatusCode.BadRequest
                }
            }

            "Fant ikke identitetsnummer resulterer i 404 not found" {
                coEvery { brukerstoetteService.hentDetaljer(any()) } throws FantIkkeIdentitetsnummer("Fant ikke identitetsnummer for periodeId")

                testApplication {
                    configureTestApplication(testApplicationContext.toApplicationContext())
                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(DetaljerRequest("12345678901"))
                    }

                    response.status shouldBe HttpStatusCode.NotFound
                    val responseBody = response.body<no.nav.paw.error.model.ProblemDetails>()
                    responseBody.detail shouldBe "Fant ikke identitetsnummer for periodeId"
                    responseBody.status shouldBe HttpStatusCode.NotFound
                }
            }

            "Ingen svar fra Oppslag gir likevel 200 OK" {
                coEvery {
                    kafkaKeysClient.getIdAndKeyOrNull("12345678901")
                } returns KafkaKeysResponse(id = 1, key = 1L)

                every {
                    hendelseRecordRepository.readAllNestedRecordsForId(any(), any(), any(), any())
                } returns listOf(
                    avvist(
                        identitetsnummer = "12345678901",
                        id = 1
                    ).storedHendelseRecord(arbeidssoekerId = 1)
                )

                coEvery {
                    oppslagApiClient.perioder("12345678901")
                } returns emptyList<ArbeidssoekerperiodeResponse>().right()

                coEvery {
                    oppslagApiClient.opplysninger("12345678901", any())
                } returns emptyList<OpplysningerOmArbeidssoekerResponse>().right()

                coEvery {
                    oppslagApiClient.profileringer("12345678901", any())
                } returns emptyList<ProfileringResponse>().right()

                testApplication {
                    configureTestApplication(testApplicationContext.toApplicationContext())
                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(DetaljerRequest("12345678901"))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val detaljer: DetaljerResponse = response.body()
                    detaljer.arbeidssoekerId shouldBe 1
                }
            }

            "Svar fra Oppslag gir 200 OK" {
                val startet = startet(identitetsnummer = "12345678901", id = 1)
                val opplysninger = opplysninger(identitetsnummer = "12345678901", id = 1)
                coEvery {
                    kafkaKeysClient.getIdAndKeyOrNull("12345678901")
                } returns KafkaKeysResponse(id = 1, key = 1L)

                every {
                    hendelseRecordRepository.readAllNestedRecordsForId(any(), any(), any(), any())
                } returns listOf(
                    startet.storedHendelseRecord(arbeidssoekerId = 1),
                    opplysninger.storedHendelseRecord(arbeidssoekerId = 1),
                )

                coEvery {
                    oppslagApiClient.perioder("12345678901")
                } returns listOf(
                    ArbeidssoekerperiodeResponse(
                        periodeId = startet.hendelseId,
                        startet = MetadataResponse(
                            tidspunkt = startet.metadata.tidspunkt,
                            utfoertAv = BrukerResponse(
                                type = BrukerType.SLUTTBRUKER,
                                id = startet.metadata.utfoertAv.id,
                            ),
                            kilde = startet.metadata.kilde,
                            aarsak = startet.metadata.aarsak,
                            tidspunktFraKilde = null,

                            ),
                        avsluttet = null
                    )
                ).right()

                coEvery {
                    oppslagApiClient.opplysninger("12345678901", any())
                } returns emptyList<OpplysningerOmArbeidssoekerResponse>().right()

                coEvery {
                    oppslagApiClient.profileringer("12345678901", any())
                } returns emptyList<ProfileringResponse>().right()

                testApplication {
                    configureTestApplication(testApplicationContext.toApplicationContext())
                    val client = configureTestClient()
                    val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                        headers {
                            append("Content-Type", "application/json")
                        }
                        setBody(DetaljerRequest("12345678901"))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val detaljer: DetaljerResponse = response.body()
                    detaljer.arbeidssoekerId shouldBe 1
                    detaljer.historikk.isNotEmpty() shouldBe true
                    detaljer.gjeldeneTilstand.shouldNotBeNull()
                    detaljer.gjeldeneTilstand.harAktivePeriode shouldBe true
                }
            }
        }
    }
})
