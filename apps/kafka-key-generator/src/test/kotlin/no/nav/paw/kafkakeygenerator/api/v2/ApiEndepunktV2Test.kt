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
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.TestContext.Companion.setJsonBody
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition

class ApiEndepunktV2Test : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val identitetRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)

        beforeSpec {
            setUp()
            mockOAuth2Server.start()
        }
        beforeEach { clearAllMocks() }
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

        "Skal opprette identiteter for person" {
            testApplication {
                configureWebApplication()
                val client = buildTestClient()
                val token = mockOAuth2Server.issueAzureToken()

                // GIVEN
                val aktorId = TestData.aktorId4
                val npId = TestData.npId4
                val dnr = TestData.dnr4
                val fnr = TestData.fnr4_1
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                // WHEN
                val response1 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = dnr.identitet))
                }

                // THEN
                response1.status shouldBe HttpStatusCode.OK
                val body1 = response1.body<ResponseV2>()
                body1.id shouldBe 1
                body1.key shouldBe publicTopicKeyFunction(ArbeidssoekerId(body1.id)).value

                val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
                kafkaKeyRow1 shouldNotBe null
                kafkaKeyRow1!!.identitetsnummer shouldBe dnr.identitet
                kafkaKeyRow1.arbeidssoekerId shouldBe body1.id

                val identitetRow1 = identitetRepository.getByIdentitet(dnr.identitet)
                identitetRow1 shouldNotBe null
                identitetRow1!!.aktorId shouldBe aktorId.identitet
                identitetRow1.identitet shouldBe dnr.identitet
                val identitetRows1 = identitetRepository.findByAktorId(identitetRow1.aktorId)
                identitetRows1.size shouldBe 3
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                // WHEN
                val response2 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = fnr.identitet))
                }

                // THEN
                response2.status shouldBe HttpStatusCode.OK
                val body2 = response2.body<ResponseV2>()
                body2.id shouldBe body1.id
                body2.key shouldBe body1.key

                val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
                kafkaKeyRow2 shouldNotBe null
                kafkaKeyRow2!!.identitetsnummer shouldBe fnr.identitet
                kafkaKeyRow2.arbeidssoekerId shouldBe body2.id

                val identitetRow2 = identitetRepository.getByIdentitet(fnr.identitet)
                identitetRow2 shouldNotBe null
                identitetRow2!!.aktorId shouldBe aktorId.identitet
                identitetRow2.identitet shouldBe fnr.identitet
                val identitetRows2 = identitetRepository.findByAktorId(identitetRow2.aktorId)
                identitetRows2.size shouldBe 4
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = fnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 2
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                identitetRecord1.key() shouldBe body1.id
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr,
                        body1.id.asIdentitet(gjeldende = true)
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
                identitetRecord2.key() shouldBe body1.id
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr,
                        body1.id.asIdentitet(gjeldende = true)
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr,
                        body1.id.asIdentitet(gjeldende = true)
                    )
                }
            }
        }
    }
})