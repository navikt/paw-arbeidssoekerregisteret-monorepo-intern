package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.api.v2.hentLokaleAlias
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.GenericFailure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Left
import no.nav.paw.kafkakeygenerator.vo.Right
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.fail

class KafkaKeysServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val identitetRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)

        beforeSpec { setUp() }
        beforeEach { clearAllMocks() }
        afterTest {
            verify { pawHendelseloggProducerMock wasNot Called }
            confirmVerified(
                pawIdentitetProducerMock,
                pawHendelseloggProducerMock
            )
        }
        afterSpec { tearDown() }

        "Test suite for hentEllerOpprett()" - {
            "Skal gi samme nøkkel for alle identer for person1" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId1, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId1, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr1_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr1_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val identiteter = listOf(aktorId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet)
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                // WHEN
                val kafkaKeys = identiteter
                    .map(::hentEllerOpprett)

                // THEN
                kafkaKeys.filterIsInstance<Left<GenericFailure>>().size shouldBe 0
                val arbeidssoekerIdList = kafkaKeys.filterIsInstance<Right<ArbeidssoekerId>>().map { it.right }
                arbeidssoekerIdList.distinct().size shouldBe 1

                kafkaKeysService.hentLokaleAlias(2, listOf(dnr.identitet))
                    .onLeft { fail { "Uventet feil: $it" } }
                    .onRight { res ->
                        res
                            .flatMap { it.koblinger }
                            .map { it.identitetsnummer } shouldContainOnly identiteter
                    }
                val lokaleAlias = kafkaKeysService.hentLokaleAlias(2, Identitetsnummer(dnr.identitet))
                lokaleAlias.onLeft { fail { "Uventet feil: $it" } }.onRight { alias ->
                    alias.identitetsnummer shouldBe dnr.identitet
                    alias.koblinger
                        .map { it.identitetsnummer } shouldContainOnly identiteter
                }

                val arbeidssoekerId = arbeidssoekerIdList.first().value
                val arbId = arbeidssoekerId.asIdentitet(gjeldende = true)
                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 3

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
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                verify(exactly = 1) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 1
                val identitetRecord1 = identitetProducerRecordList[0]
                identitetRecord1.key() shouldBe arbeidssoekerId
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId)
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal gi samme nøkkel for alle identer for person2" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId2, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId2, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr2, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr2_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr2_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val identiteter = listOf(aktorId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet)
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                // WHEN
                val kafkaKeys = identiteter
                    .map(::hentEllerOpprett)

                // THEN
                kafkaKeys.filterIsInstance<Left<GenericFailure>>().size shouldBe 0
                val arbeidssoekerIdList = kafkaKeys.filterIsInstance<Right<ArbeidssoekerId>>().map { it.right }
                arbeidssoekerIdList.distinct().size shouldBe 1

                val arbeidssoekerId = arbeidssoekerIdList.first().value
                val arbId = arbeidssoekerId.asIdentitet(gjeldende = true)
                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 3

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
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                verify(exactly = 1) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 1
                val identitetRecord1 = identitetProducerRecordList[0]
                identitetRecord1.key() shouldBe arbeidssoekerId
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId)
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal gi forskjellig nøkkel for person1 og person2" {
                hentEllerOpprett(TestData.aktorId1) shouldNotBe hentEllerOpprett(TestData.aktorId2)
                hentEllerOpprett(TestData.dnr1) shouldNotBe hentEllerOpprett(TestData.dnr2)
                hentEllerOpprett(TestData.dnr1) shouldNotBe hentEllerOpprett(TestData.aktorId2)
                hentEllerOpprett(TestData.dnr2) shouldNotBe hentEllerOpprett(TestData.aktorId1)

                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal gi ingen treff for ukjent person i PDL" {
                val person3KafkaKeys = listOf(
                    TestData.dnr6,
                    TestData.fnr5_1
                ).map(::hentEllerOpprett)
                person3KafkaKeys.filterIsInstance<Left<Failure>>().size shouldBe 2
                person3KafkaKeys.filterIsInstance<Right<Long>>().size shouldBe 0
                person3KafkaKeys.forEach {
                    it.shouldBeInstanceOf<Left<Failure>>()
                    it.left.code() shouldBe FailureCode.PDL_NOT_FOUND
                }

                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for hent()" - {
        }
    }
})
