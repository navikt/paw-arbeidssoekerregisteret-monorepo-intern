package no.nav.paw.kafkakeygenerator.api.v2

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.api.models.IdentitetRequest
import no.nav.paw.kafkakeygenerator.api.models.IdentitetResponse
import no.nav.paw.kafkakeygenerator.api.models.Konflikt
import no.nav.paw.kafkakeygenerator.api.models.KonfliktDetaljer
import no.nav.paw.kafkakeygenerator.api.models.ProblemDetails
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.TestContext.Companion.setJsonBody
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asApi
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.test.validateOpenApiSpec
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant

class ApiV2RoutesTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val identitetRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)
        val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

        beforeSpec {
            setUp()
            mockOAuth2Server.start()
        }
        beforeTest {
            identitetProducerRecordList.clear()
            clearAllMocks()
            every {
                pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
            } returns identitetRecordMetadata
        }
        afterTest {
            verify { pawHendelseloggProducerMock wasNot Called }
            confirmVerified(
                pawIdentitetProducerMock,
                pawHendelseloggProducerMock
            )
        }
        afterSpec {
            tearDown()
            mockOAuth2Server.shutdown()
        }

        "Test suite for hent" - {
            "Skal kaste feil for person som ikke er arbeidssøker" {
                testApplication {
                    configureWebApplication()
                    val client = buildTestClient()
                    val token = mockOAuth2Server.issueAzureToken()

                    // GIVEN
                    val aktorId = TestData.aktorId1
                    val dnr = TestData.dnr1
                    val fnr = TestData.fnr1_1

                    // WHEN
                    val response1 = client.post("/api/v2/hent") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = aktorId.identitet))
                    }
                    val response2 = client.post("/api/v2/hent") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = dnr.identitet))
                    }
                    val response3 = client.post("/api/v2/hent") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = fnr.identitet))
                    }

                    // THEN
                    response1.status shouldBe HttpStatusCode.NotFound
                    val body1 = response1.body<ProblemDetails>()
                    body1.type.toString() shouldBe "urn:paw:identiteter:identitet-ikke-funnet"
                    body1.status shouldBe 404
                    response2.status shouldBe HttpStatusCode.NotFound
                    val body2 = response2.body<ProblemDetails>()
                    body2.type.toString() shouldBe "urn:paw:identiteter:identitet-ikke-funnet"
                    body2.status shouldBe 404
                    response3.status shouldBe HttpStatusCode.NotFound
                    val body3 = response3.body<ProblemDetails>()
                    body3.type.toString() shouldBe "urn:paw:identiteter:identitet-ikke-funnet"
                    body3.status shouldBe 404

                    verify { pawIdentitetProducerMock wasNot Called }
                }
            }

            "Skal hente keys" {
                testApplication {
                    configureWebApplication()
                    val client = buildTestClient()
                    val token = mockOAuth2Server.issueAzureToken()

                    // GIVEN
                    val aktorId = TestData.aktorId2
                    val npId = TestData.npId2
                    val dnr = TestData.dnr2
                    val fnr1 = TestData.fnr2_1
                    val fnr2 = TestData.fnr2_2
                    val arbeidssoekerId = KafkaKeysTable.insert().value
                    val arbId = arbeidssoekerId.asIdentitet()
                    val recordKey = arbeidssoekerId.asRecordKey()

                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId.identitet,
                        identitet = aktorId.identitet,
                        type = IdentitetType.AKTORID,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now()
                    )
                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId.identitet,
                        identitet = dnr.identitet,
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now()
                    )

                    // WHEN
                    val response1 = client.post("/api/v2/hent") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = dnr.identitet))
                    }
                    val response2 = client.post("/api/v2/hent") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = fnr2.identitet))
                    }

                    // THEN
                    response1.status shouldBe HttpStatusCode.OK
                    val body1 = response1.body<ResponseV2>()
                    body1.id shouldBe arbeidssoekerId
                    body1.key shouldBe recordKey
                    response2.status shouldBe HttpStatusCode.OK
                    val body2 = response1.body<ResponseV2>()
                    body2.id shouldBe arbeidssoekerId
                    body2.key shouldBe recordKey

                    val identitetRows = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows.size shouldBe 5
                    identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = npId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr1.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr2,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                    identitetProducerRecordList shouldHaveSize 1
                    val identitetRecord1 = identitetProducerRecordList[0]
                    identitetRecord1.key() shouldBe arbeidssoekerId
                    identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                        hendelse.identiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr.copy(gjeldende = false),
                            fnr1.copy(gjeldende = false),
                            fnr2,
                            arbId
                        )
                        hendelse.tidligereIdentiteter shouldContainOnly listOf(
                            aktorId,
                            dnr,
                            arbId
                        )
                    }
                }
            }
        }

        "Test suite for hentEllerOpprett" - {
            "Skal hente keys for person som ikke er arbeidssøker" {
                testApplication {
                    configureWebApplication()
                    val client = buildTestClient()
                    val token = mockOAuth2Server.issueAzureToken()

                    // GIVEN
                    val aktorId = TestData.aktorId3
                    val npId = TestData.npId3
                    val dnr = TestData.dnr3
                    val fnr = TestData.fnr3_1

                    // WHEN
                    val response1 = client.post("/api/v2/hentEllerOpprett") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = dnr.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    val arbeidssoekerId = TestData.hentArbeidssoekerId(aktorId.identitet)
                    val arbId = arbeidssoekerId.asIdentitet()
                    val recordKey = arbeidssoekerId.asRecordKey()

                    response1.status shouldBe HttpStatusCode.OK
                    val body1 = response1.body<ResponseV2>()
                    body1.id shouldBe arbeidssoekerId
                    body1.key shouldBe recordKey

                    val identitetRows1 = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows1.size shouldBe 3
                    identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = npId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    // WHEN
                    val response2 = client.post("/api/v2/hentEllerOpprett") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = fnr.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    response2.status shouldBe HttpStatusCode.OK
                    val body2 = response2.body<ResponseV2>()
                    body2.id shouldBe arbeidssoekerId
                    body2.key shouldBe recordKey

                    val identitetRows2 = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows2.size shouldBe 4
                    identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = npId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                    identitetProducerRecordList shouldHaveSize 2
                    val identitetRecord1 = identitetProducerRecordList[0]
                    val identitetRecord2 = identitetProducerRecordList[1]
                    identitetRecord1.key() shouldBe arbeidssoekerId
                    identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                        hendelse.identiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr,
                            arbId
                        )
                        hendelse.tidligereIdentiteter shouldBe emptyList()
                    }
                    identitetRecord2.key() shouldBe arbeidssoekerId
                    identitetRecord2.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                        hendelse.identiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr.copy(gjeldende = false),
                            fnr,
                            arbId
                        )
                        hendelse.tidligereIdentiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr,
                            arbId
                        )
                    }
                }
            }

            "Skal hente keys for person som er arbeidssøker" {
                testApplication {
                    configureWebApplication()
                    val client = buildTestClient()
                    val token = mockOAuth2Server.issueAzureToken()

                    // GIVEN
                    val aktorId = TestData.aktorId4
                    val npId = TestData.npId4
                    val dnr = TestData.dnr4
                    val fnr1 = TestData.fnr4_1
                    val fnr2 = TestData.fnr4_2

                    val arbeidssoekerId = KafkaKeysTable.insert().value
                    val arbId = arbeidssoekerId.asIdentitet()
                    val recordKey = arbeidssoekerId.asRecordKey()

                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId.identitet,
                        identitet = aktorId.identitet,
                        type = IdentitetType.AKTORID,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now()
                    )
                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId,
                        aktorId = aktorId.identitet,
                        identitet = dnr.identitet,
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now()
                    )

                    // WHEN
                    val response1 = client.post("/api/v2/hentEllerOpprett") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = dnr.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    response1.status shouldBe HttpStatusCode.OK
                    val body1 = response1.body<ResponseV2>()
                    body1.id shouldBe arbeidssoekerId
                    body1.key shouldBe recordKey

                    val identitetRows1 = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows1.size shouldBe 2
                    identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    // WHEN
                    val response2 = client.post("/api/v2/hentEllerOpprett") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = fnr1.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    response2.status shouldBe HttpStatusCode.OK
                    val body2 = response2.body<ResponseV2>()
                    body2.id shouldBe arbeidssoekerId
                    body2.key shouldBe recordKey

                    val identitetRows2 = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows2.size shouldBe 4
                    identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = npId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr1,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    // WHEN
                    val response3 = client.post("/api/v2/hentEllerOpprett") {
                        bearerAuth(token.serialize())
                        setJsonBody(RequestV2(ident = fnr2.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    response3.status shouldBe HttpStatusCode.OK
                    val body3 = response3.body<ResponseV2>()
                    body3.id shouldBe arbeidssoekerId
                    body3.key shouldBe recordKey

                    val identitetRows3 = IdentiteterTable.findByAktorId(aktorId.identitet)
                    identitetRows3.size shouldBe 5
                    identitetRows3.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = npId,
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = dnr.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr1.copy(gjeldende = false),
                            status = IdentitetStatus.SLETTET
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId,
                            aktorId = aktorId.identitet,
                            identitet = fnr2,
                            status = IdentitetStatus.AKTIV
                        )
                    )

                    verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                    identitetProducerRecordList shouldHaveSize 2
                    val identitetRecord1 = identitetProducerRecordList[0]
                    val identitetRecord2 = identitetProducerRecordList[1]
                    identitetRecord1.key() shouldBe arbeidssoekerId
                    identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                        hendelse.identiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr.copy(gjeldende = false),
                            fnr1,
                            arbId
                        )
                        hendelse.tidligereIdentiteter shouldContainOnly listOf(
                            aktorId,
                            dnr,
                            arbId
                        )
                    }
                    identitetRecord2.key() shouldBe arbeidssoekerId
                    identitetRecord2.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                        hendelse.identiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr.copy(gjeldende = false),
                            fnr2,
                            arbId
                        )
                        hendelse.tidligereIdentiteter shouldContainOnly listOf(
                            aktorId,
                            npId,
                            dnr.copy(gjeldende = false),
                            fnr1,
                            arbId
                        )
                    }
                }
            }
        }

        "Test suite for identiteter" - {
            "Skal hente ideniteter for person" {
                testApplication {
                    configureWebApplication()
                    val client = buildTestClient()
                    val token = mockOAuth2Server.issueAzureToken()

                    // GIVEN
                    val aktorId = TestData.aktorId5
                    val npId = TestData.npId5
                    val dnr = TestData.dnr5
                    val fnr1 = TestData.fnr5_1
                    val fnr2 = TestData.fnr5_2
                    val arbeidssoekerId1 = KafkaKeysTable.insert().value
                    val arbeidssoekerId2 = KafkaKeysTable.insert().value
                    val arbId1 = arbeidssoekerId1.asIdentitet()
                    val recordKey1 = arbeidssoekerId1.asRecordKey()

                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId.identitet,
                        type = IdentitetType.AKTORID,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                    )
                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId.identitet,
                        type = IdentitetType.NPID,
                        gjeldende = false,
                        status = IdentitetStatus.SLETTET,
                        sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                    )
                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr.identitet,
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                    )
                    IdentiteterTable.insert(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1.identitet,
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true,
                        status = IdentitetStatus.AKTIV,
                        sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                    )
                    KonflikterTable.insert(
                        aktorId = aktorId.identitet,
                        type = KonfliktType.MERGE,
                        status = KonfliktStatus.VENTER,
                        sourceTimestamp = Instant.now(),
                        identiteter = listOf(aktorId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2)
                    )

                    // WHEN
                    val response = client.post("/api/v2/identiteter?hentPdl=true&visKonflikter=true") {
                        bearerAuth(token.serialize())
                        setJsonBody(IdentitetRequest(dnr.identitet))
                    }.validateOpenApiSpec()

                    // THEN
                    response.status shouldBe HttpStatusCode.OK
                    val body = response.body<IdentitetResponse>()

                    body.arbeidssoekerId shouldBe arbeidssoekerId1
                    body.recordKey shouldBe recordKey1
                    body.identiteter shouldContainOnly listOf(
                        aktorId.asApi(),
                        dnr.asApi(),
                        arbId1.asApi()
                    )
                    body.pdlIdentiteter shouldNotBe null
                    body.pdlIdentiteter shouldContainOnly listOf(
                        aktorId.asApi(),
                        dnr.copy(gjeldende = false).asApi(),
                        fnr1.copy(gjeldende = false).asApi(),
                        fnr2.asApi()
                    )
                    body.konflikter shouldNotBe null
                    body.konflikter shouldContainOnly listOf(
                        Konflikt(
                            type = KonfliktType.MERGE.asApi(),
                            detaljer = KonfliktDetaljer(
                                aktorIdListe = listOf(
                                    aktorId.identitet
                                ),
                                arbeidssoekerIdListe = listOf(
                                    arbeidssoekerId1
                                )
                            )
                        )
                    )

                    verify { pawIdentitetProducerMock wasNot Called }
                }
            }
        }
    }
})