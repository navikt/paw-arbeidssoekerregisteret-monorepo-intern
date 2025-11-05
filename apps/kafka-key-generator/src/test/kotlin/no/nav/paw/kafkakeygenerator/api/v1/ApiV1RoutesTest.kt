package no.nav.paw.kafkakeygenerator.api.v1

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
import no.nav.paw.kafkakeygenerator.api.models.ProblemDetails
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.TestContext.Companion.setJsonBody
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Instant

class ApiV1RoutesTest : FreeSpec({
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
            confirmVerified(pawIdentitetProducerMock)
        }
        afterSpec {
            tearDown()
            mockOAuth2Server.shutdown()
        }

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
                val response1 = client.post("/api/v1/record-key") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV1(ident = aktorId.identitet))
                }
                val response2 = client.post("/api/v1/record-key") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV1(ident = dnr.identitet))
                }
                val response3 = client.post("/api/v1/record-key") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV1(ident = fnr.identitet))
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

        "Skal hente key" {
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
                val response1 = client.post("/api/v1/record-key") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV1(ident = dnr.identitet))
                }
                val response2 = client.post("/api/v1/record-key") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV1(ident = fnr2.identitet))
                }

                // THEN
                response1.status shouldBe HttpStatusCode.OK
                val body1 = response1.body<ResponseV1>()
                body1.key shouldBe recordKey
                response2.status shouldBe HttpStatusCode.OK
                val body2 = response1.body<ResponseV1>()
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
})