package no.nav.paw.bekreftelse.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
import io.mockk.verify
import no.nav.paw.bekreftelse.api.ApplicationTestContext
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.api.ApiResult

class AuthRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {

        beforeSpec {
            clearAllMocks()
            mockOAuth2Server.start()
        }

        afterSpec {
            confirmVerified(
                kafkaKeysClientMock,
                poaoTilgangClientMock
            )
            mockOAuth2Server.shutdown()
        }

        /*
         * GENERELLE TESTER
         */
        "Test suite for generell autentisering og autorisering" - {

            "Skal få 401 ved manglende Bearer Token" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val response = client.get("/api/secured")

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            "Skal få 401 ved token utstedt av ukjent issuer" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        issuerId = "whatever",
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/secured") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            "Skal få 403 ved token uten noen claims" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken()

                    val response = client.get("/api/secured") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_UGYLDIG_BEARER_TOKEN"
                }
            }
        }

        /*
         * SLUTTBRUKER TESTER
         */
        "Test suite for sluttbruker autentisering og autorisering" - {

            "Skal få 403 ved TokenX-token uten pid claim" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high"
                        )
                    )

                    val response = client.get("/api/secured") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_UGYLDIG_BEARER_TOKEN"
                }
            }

            "Skal få 403 ved TokenX-token når innsendt ident ikke er lik pid claim" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.post("/api/secured") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(TilgjengeligeBekreftelserRequest("02017012345"))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_BRUKER_HAR_IKKE_TILGANG"
                }
            }

            "Skal få 200 ved TokenX-token når innsendt ident er lik pid claim" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(1, 1)

                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.post("/api/secured") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(TilgjengeligeBekreftelserRequest("01017012345"))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldBe "WHATEVER"

                    coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                }
            }
        }


        /*
         * VEILEDER TESTER
         */
        "Test suite for veilder autentisering og autorisering" - {

            "Skal få 403 ved Azure-token men GET-request" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "oid" to "a6a2e743-acc1-4af0-b89e-d5980500fc2a",
                            "NAVident" to "12345"
                        )
                    )

                    val response = client.get("/api/secured") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_BRUKER_HAR_IKKE_TILGANG"
                }
            }

            "Skal få 403 ved Azure-token med POST-request uten ident" {
                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "oid" to "a6a2e743-acc1-4af0-b89e-d5980500fc2a",
                            "NAVident" to "12345"
                        )
                    )

                    val response = client.post("/api/secured") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(TilgjengeligeBekreftelserRequest())
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_BRUKER_HAR_IKKE_TILGANG"
                }
            }

            "Skal få 403 ved Azure-token med POST-request men uten POAO tilgang" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(1, 1)
                every { poaoTilgangClientMock.evaluatePolicy(any<NavAnsattTilgangTilEksternBrukerPolicyInput>()) } returns ApiResult(
                    throwable = null,
                    result = Decision.Deny("You shall not pass!", "Balrogs suck")
                )

                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "oid" to "a6a2e743-acc1-4af0-b89e-d5980500fc2a",
                            "NAVident" to "12345"
                        )
                    )

                    val response = client.post("/api/secured") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(TilgjengeligeBekreftelserRequest("01017012345"))
                    }

                    response.status shouldBe HttpStatusCode.Forbidden
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.Forbidden
                    body.code shouldBe "PAW_BRUKER_HAR_IKKE_TILGANG"

                    coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                    verify { poaoTilgangClientMock.evaluatePolicy(any<NavAnsattTilgangTilEksternBrukerPolicyInput>()) }
                }
            }

            "Skal få 200 ved Azure-token med POST-request og med POAO tilgang" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(1, 1)
                every { poaoTilgangClientMock.evaluatePolicy(any<NavAnsattTilgangTilEksternBrukerPolicyInput>()) } returns ApiResult(
                    throwable = null,
                    result = Decision.Permit
                )

                testApplication {
                    configureTestApplication(bekreftelseServiceMock)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "oid" to "a6a2e743-acc1-4af0-b89e-d5980500fc2a",
                            "NAVident" to "12345"
                        )
                    )

                    val response = client.post("/api/secured") {
                        bearerAuth(token.serialize())
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json)
                        }
                        setBody(TilgjengeligeBekreftelserRequest("01017012345"))
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.bodyAsText()
                    body shouldBe "WHATEVER"

                    coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
                    verify { poaoTilgangClientMock.evaluatePolicy(any<NavAnsattTilgangTilEksternBrukerPolicyInput>()) }
                }
            }
        }
    }
})