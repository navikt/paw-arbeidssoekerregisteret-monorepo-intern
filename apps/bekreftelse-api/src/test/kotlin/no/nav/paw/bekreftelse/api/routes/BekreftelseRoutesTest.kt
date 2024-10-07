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
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters

class BekreftelseRoutesTest : FreeSpec({
    with(ApplicationTestContext()) {
        beforeSpec {
            clearAllMocks()
            mockOAuth2Server.start()
        }

        afterSpec {
            coVerify { kafkaKeysClientMock.getIdAndKey(any<String>()) }
            confirmVerified(
                kafkaStreamsMock,
                stateStoreMock,
                kafkaKeysClientMock,
                poaoTilgangClientMock,
                bekreftelseKafkaProducerMock,
                bekreftelseHttpConsumerMock,
                bekreftelseServiceMock
            )
            mockOAuth2Server.shutdown()
        }

        /*
        * SLUTTBRUKER TESTER
        */
        "Test suite for sluttbruker" - {
            "Skal få 500 om Kafka Streams ikke kjører" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.NOT_RUNNING

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.InternalServerError
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.InternalServerError
                    body.code shouldBe "PAW_SYSTEMFEIL"
                    verify { kafkaStreamsMock.state() }
                }
            }

            "Skal få 500 om ingen state store funnet" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns null

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.InternalServerError
                    val body = response.body<ProblemDetails>()
                    body.status shouldBe HttpStatusCode.InternalServerError
                    body.code shouldBe "PAW_SYSTEMFEIL"
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                }
            }

            "Skal hente tilgjengelige bekreftelser fra intern state store" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns InternState(
                    listOf(
                        testData.nyBekreftelseTilgjengelig()
                    )
                )

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 1
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify { stateStoreMock.get(any<Long>()) }
                }
            }

            "Skal hente tilgjengelige bekreftelser fra annen node" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns null
                every {
                    kafkaStreamsMock.queryMetadataForKey(
                        any<String>(),
                        any<Long>(),
                        any<Serializer<Long>>()
                    )
                } returns testData.nyKeyQueryMetadata()
                coEvery {
                    bekreftelseHttpConsumerMock.finnTilgjengeligBekreftelser(
                        any<String>(),
                        any<String>(),
                        any<TilgjengeligeBekreftelserRequest>()
                    )
                } returns listOf(testData.nyTilgjengeligBekreftelse())

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 1
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify {
                        kafkaStreamsMock.queryMetadataForKey(
                            any<String>(),
                            any<Long>(),
                            any<Serializer<Long>>()
                        )
                    }
                    verify { stateStoreMock.get(any<Long>()) }
                    coVerify {
                        bekreftelseHttpConsumerMock.finnTilgjengeligBekreftelser(
                            any<String>(),
                            any<String>(),
                            any<TilgjengeligeBekreftelserRequest>()
                        )
                    }
                }
            }

            "Skal hente tilgjengelige bekreftelser men finner ingen" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns null
                every {
                    kafkaStreamsMock.queryMetadataForKey(
                        any<String>(),
                        any<Long>(),
                        any<Serializer<Long>>()
                    )
                } returns testData.nyKeyQueryMetadata(host = "127.0.0.1")
                coEvery {
                    bekreftelseHttpConsumerMock.finnTilgjengeligBekreftelser(
                        any<String>(),
                        any<String>(),
                        any<TilgjengeligeBekreftelserRequest>()
                    )
                } returns listOf(testData.nyTilgjengeligBekreftelse())

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to "01017012345"
                        )
                    )

                    val response = client.get("/api/v1/tilgjengelige-bekreftelser") {
                        bearerAuth(token.serialize())
                    }

                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<TilgjengeligBekreftelserResponse>()
                    body.size shouldBe 0
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify {
                        kafkaStreamsMock.queryMetadataForKey(
                            any<String>(),
                            any<Long>(),
                            any<Serializer<Long>>()
                        )
                    }
                    verify { stateStoreMock.get(any<Long>()) }
                    coVerify {
                        bekreftelseHttpConsumerMock.finnTilgjengeligBekreftelser(
                            any<String>(),
                            any<String>(),
                            any<TilgjengeligeBekreftelserRequest>()
                        )
                    }
                }
            }

            "Skal motta bekreftelse via intern state store" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId1,
                    testData.kafkaKey1
                )
                val request = testData.nyBekreftelseRequest(identitetsnummer = testData.fnr1)
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns InternState(
                    listOf(
                        testData.nyBekreftelseTilgjengelig(
                            bekreftelseId = request.bekreftelseId,
                            arbeidsoekerId = testData.arbeidsoekerId1
                        )
                    )
                )
                coEvery {
                    bekreftelseKafkaProducerMock.produceMessage(
                        any<Long>(),
                        any<Bekreftelse>()
                    )
                } just runs

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
                    val client = configureTestClient()

                    val token = mockOAuth2Server.issueToken(
                        claims = mapOf(
                            "acr" to "idporten-loa-high",
                            "pid" to testData.fnr1
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
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify { stateStoreMock.get(any<Long>()) }
                    coVerify {
                        bekreftelseKafkaProducerMock.produceMessage(
                            any<Long>(),
                            any<Bekreftelse>()
                        )
                    }
                }
            }

            "Skal motta bekreftelse og overføre den til annen node" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId2,
                    testData.kafkaKey2
                )
                val request = testData.nyBekreftelseRequest(identitetsnummer = testData.fnr2)
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns null
                every {
                    kafkaStreamsMock.queryMetadataForKey(
                        any<String>(),
                        any<Long>(),
                        any<Serializer<Long>>()
                    )
                } returns testData.nyKeyQueryMetadata()
                coEvery {
                    bekreftelseHttpConsumerMock.sendBekreftelse(
                        any<String>(),
                        any<String>(),
                        any<BekreftelseRequest>()
                    )
                } just runs

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
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
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify {
                        kafkaStreamsMock.queryMetadataForKey(
                            any<String>(),
                            any<Long>(),
                            any<Serializer<Long>>()
                        )
                    }
                    verify { stateStoreMock.get(any<Long>()) }
                    coVerify {
                        bekreftelseHttpConsumerMock.sendBekreftelse(
                            any<String>(),
                            any<String>(),
                            any<BekreftelseRequest>()
                        )
                    }
                }
            }

            "Skal motta bekreftelse men finner ikke relatert tilgjengelig bekreftelse" {
                coEvery { kafkaKeysClientMock.getIdAndKey(any<String>()) } returns KafkaKeysResponse(
                    testData.arbeidsoekerId2,
                    testData.kafkaKey2
                )
                val request = testData.nyBekreftelseRequest(identitetsnummer = testData.fnr2)
                every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
                every { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) } returns stateStoreMock
                every { stateStoreMock.get(any<Long>()) } returns null
                every {
                    kafkaStreamsMock.queryMetadataForKey(
                        any<String>(),
                        any<Long>(),
                        any<Serializer<Long>>()
                    )
                } returns testData.nyKeyQueryMetadata(host = "127.0.0.1")
                coEvery {
                    bekreftelseHttpConsumerMock.sendBekreftelse(
                        any<String>(),
                        any<String>(),
                        any<BekreftelseRequest>()
                    )
                } just runs

                testApplication {
                    configureTestApplication(bekreftelseServiceReal)
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

                    response.status shouldBe HttpStatusCode.BadRequest
                    val body = response.body<ProblemDetails>()
                    body.code shouldBe "PAW_DATA_IKKE_FUNNET_FOR_ID"
                    verify { kafkaStreamsMock.state() }
                    verify { kafkaStreamsMock.store(any<StoreQueryParameters<*>>()) }
                    verify {
                        kafkaStreamsMock.queryMetadataForKey(
                            any<String>(),
                            any<Long>(),
                            any<Serializer<Long>>()
                        )
                    }
                    verify { stateStoreMock.get(any<Long>()) }
                    coVerify {
                        bekreftelseHttpConsumerMock.sendBekreftelse(
                            any<String>(),
                            any<String>(),
                            any<BekreftelseRequest>()
                        )
                    }
                }
            }
        }
    }
})