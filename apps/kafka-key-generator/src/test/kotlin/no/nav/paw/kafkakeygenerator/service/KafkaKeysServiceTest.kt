package no.nav.paw.kafkakeygenerator.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
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
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.hentEllerOppdater
import no.nav.paw.kafkakeygenerator.context.hentEllerOpprett
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.exception.PdlUkjentIdentitetException
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Instant

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
                val aktorId = TestData.aktorId1
                val npId = TestData.npId1
                val dnr = TestData.dnr1
                val fnr1 = TestData.fnr1_1
                val fnr2 = TestData.fnr1_2
                val identer = TestData.aktor1_3.identifikatorer.map { it.idnummer }
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                // WHEN
                val arbeidssoekerIdList = identer
                    .map { kafkaKeysService.hentEllerOpprett(it) }

                // THEN
                arbeidssoekerIdList.distinct().size shouldBe 1

                val lokaleAliasList = kafkaKeysService.hentLokaleAlias(2, listOf(dnr.identitet))
                lokaleAliasList
                    .flatMap {
                        it.koblinger.map { k -> k.identitetsnummer }
                    } shouldContainOnly identer
                val lokaleAlias = kafkaKeysService.hentLokaleAlias(2, dnr.identitet)
                lokaleAlias.identitetsnummer shouldBe dnr.identitet
                lokaleAlias.koblinger.map { it.identitetsnummer } shouldContainOnly identer

                val arbeidssoekerId = arbeidssoekerIdList.first()
                val arbId = arbeidssoekerId.asIdentitet()
                val identitetRows = IdentiteterTable.findByAktorId(aktorId.identitet)
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

                verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 2
                val identitetRecord1 = identitetProducerRecordList[0]
                identitetRecord1.key() shouldBe arbeidssoekerId
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId)
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
                val identitetRecord2 = identitetProducerRecordList[1]
                identitetRecord2.key() shouldBe arbeidssoekerId
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1.copy(gjeldende = false),
                        fnr2,
                        arbId
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId)
                }
            }

            "Skal gi samme nøkkel for alle identer for person2" {
                // GIVEN
                val aktorId = TestData.aktorId2
                val npId = TestData.npId2
                val dnr = TestData.dnr2
                val fnr1 = TestData.fnr2_1
                val fnr2 = TestData.fnr2_2
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                // WHEN
                val arbeidssoekerIdList1 = TestData.aktor2_1.identifikatorer
                    .map { kafkaKeysService.hentEllerOpprett(it.idnummer) }

                // THEN
                arbeidssoekerIdList1.distinct().size shouldBe 1
                val arbeidssoekerId1 = arbeidssoekerIdList1.first()
                val arbId1 = arbeidssoekerId1.asIdentitet()

                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId.identitet)
                identitetRows1 shouldHaveSize 3

                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                // WHEN
                val arbeidssoekerIdList2 = TestData.aktor2_2.identifikatorer
                    .map { kafkaKeysService.hentEllerOpprett(it.idnummer) }

                // THEN
                arbeidssoekerIdList2.distinct().size shouldBe 1
                val arbeidssoekerId2 = arbeidssoekerIdList2.first()
                arbeidssoekerId1 shouldBe arbeidssoekerId2

                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId.identitet)
                identitetRows2 shouldHaveSize 4

                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.AKTIV
                    )
                )

                // WHEN
                val arbeidssoekerIdList3 = TestData.aktor2_3.identifikatorer
                    .map { kafkaKeysService.hentEllerOpprett(it.idnummer) }

                // THEN
                arbeidssoekerIdList3.distinct().size shouldBe 1
                val arbeidssoekerId3 = arbeidssoekerIdList3.first()
                arbeidssoekerId1 shouldBe arbeidssoekerId3

                val identitetRows3 = IdentiteterTable.findByAktorId(aktorId.identitet)
                identitetRows3 shouldHaveSize 5

                identitetRows3.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = fnr1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                verify(exactly = 3) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 3
                val identitetRecord1 = identitetProducerRecordList[0]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
                val identitetRecord2 = identitetProducerRecordList[1]
                identitetRecord2.key() shouldBe arbeidssoekerId1
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1,
                        arbId1
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                }
                val identitetRecord3 = identitetProducerRecordList[2]
                identitetRecord3.key() shouldBe arbeidssoekerId1
                identitetRecord3.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1.copy(gjeldende = false),
                        fnr2,
                        arbId1
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1,
                        arbId1
                    )
                }
            }

            "Skal gi forskjellig nøkkel for person1 og person2" {
                kafkaKeysService.hentEllerOpprett(TestData.aktorId1.identitet) shouldNotBe kafkaKeysService.hentEllerOpprett(
                    TestData.aktorId2.identitet
                )
                kafkaKeysService.hentEllerOpprett(TestData.dnr1.identitet) shouldNotBe kafkaKeysService.hentEllerOpprett(
                    TestData.dnr2.identitet
                )
                kafkaKeysService.hentEllerOpprett(TestData.dnr1.identitet) shouldNotBe kafkaKeysService.hentEllerOpprett(
                    TestData.aktorId2.identitet
                )
                kafkaKeysService.hentEllerOpprett(TestData.dnr2.identitet) shouldNotBe kafkaKeysService.hentEllerOpprett(
                    TestData.aktorId1.identitet
                )

                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal kaste feil for ukjent person i PDL" {
                shouldThrow<PdlUkjentIdentitetException> {
                    kafkaKeysService.hentEllerOpprett(TestData.dnr6.identitet)
                }
                shouldThrow<PdlUkjentIdentitetException> {
                    kafkaKeysService.hentEllerOpprett(TestData.fnr5_1.identitet)
                }

                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for hentEllerOppdater()" - {
            "Skal kaste feil hvis identitet ikke finnes" {
                // GIVEN
                val aktorId = TestData.aktorId3
                val npId = TestData.npId3
                val dnr = TestData.dnr3
                val fnr1 = TestData.fnr3_1
                val fnr2 = TestData.fnr3_2

                shouldThrow<IdentitetIkkeFunnetException> {
                    kafkaKeysService.hentEllerOppdater(aktorId.identitet)
                }
                shouldThrow<IdentitetIkkeFunnetException> {
                    kafkaKeysService.hentEllerOppdater(npId.identitet)
                }
                shouldThrow<IdentitetIkkeFunnetException> {
                    kafkaKeysService.hentEllerOppdater(dnr.identitet)
                }
                shouldThrow<IdentitetIkkeFunnetException> {
                    kafkaKeysService.hentEllerOppdater(fnr1.identitet)
                }
                shouldThrow<IdentitetIkkeFunnetException> {
                    kafkaKeysService.hentEllerOppdater(fnr2.identitet)
                }

                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal gi samme nøkkel for alle identer for person1" {
                // GIVEN
                val aktorId = TestData.aktorId4
                val npId = TestData.npId4
                val dnr = TestData.dnr4
                val fnr1 = TestData.fnr4_1
                val fnr2 = TestData.fnr4_2
                val arbeidssoekerId = KafkaKeysTable.insert().value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata

                IdentiteterTable.insert(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.AKTIV,
                    sourceTimestamp = Instant.now()
                )
                IdentiteterTable.insert(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.AKTIV,
                    sourceTimestamp = Instant.now()
                )
                IdentiteterTable.insert(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.AKTIV,
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                val arbeidssoekerIdList = listOf(aktorId, npId, dnr, fnr1, fnr2)
                    .map { kafkaKeysService.hentEllerOppdater(it.identitet) }

                // THEN
                arbeidssoekerIdList.distinct().size shouldBe 1
                arbeidssoekerIdList shouldContain arbeidssoekerId

                val lokaleAliasList = kafkaKeysService.hentLokaleAlias(2, listOf(dnr.identitet))
                lokaleAliasList
                    .flatMap {
                        it.koblinger.map { k -> k.identitetsnummer }
                    } shouldContainOnly listOf(aktorId, npId, dnr, fnr2).map { it.identitet }
                val lokaleAlias = kafkaKeysService.hentLokaleAlias(2, dnr.identitet)
                lokaleAlias.identitetsnummer shouldBe dnr.identitet
                lokaleAlias.koblinger
                    .map { it.identitetsnummer } shouldContainOnly listOf(aktorId, npId, dnr, fnr2)
                    .map { it.identitet }

                val arbId = arbeidssoekerId.asIdentitet()
                val identitetRows = IdentiteterTable.findByAktorId(aktorId.identitet)
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
                        npId,
                        dnr,
                        arbId
                    )
                }
                val identitetRecord2 = identitetProducerRecordList[1]
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

            "Skal kaste feil for ukjent person i PDL" {
                shouldThrow<PdlUkjentIdentitetException> {
                    kafkaKeysService.hentEllerOppdater(TestData.dnr6.identitet)
                }
                shouldThrow<PdlUkjentIdentitetException> {
                    kafkaKeysService.hentEllerOppdater(TestData.fnr5_1.identitet)
                }

                verify { pawIdentitetProducerMock wasNot Called }
            }
        }
    }
})
