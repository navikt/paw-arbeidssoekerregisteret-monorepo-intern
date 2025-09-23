package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSlettetHendelse
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.KonfliktWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Instant

class IdentitetServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres().setUp()) {
        val aktorId1 = TestData.aktorId1
        val dnr1 = TestData.dnr1
        val fnr1_1 = TestData.fnr1_1
        val fnr1_2 = TestData.fnr1_2
        val aktorId2 = TestData.aktorId2
        val dnr2 = TestData.dnr2
        val fnr2_1 = TestData.fnr2_1
        val fnr2_2 = TestData.fnr2_2
        val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(TestData.dnr2.identitet))
            .fold(onLeft = { null }, onRight = { it })!!.value
        val arbId2 = arbeidssoekerId2.asIdentitet(gjeldende = true)
        val aktorId3 = TestData.aktorId3
        val dnr3 = TestData.dnr3
        val fnr3_1 = TestData.fnr3_1
        val aktorId6 = TestData.aktorId6
        val dnr6 = TestData.dnr6
        val fnr6_1 = TestData.fnr6_1
        val fnr6_2 = TestData.fnr6_2
        val arbeidssoekerId6_1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr6.identitet))
            .fold(onLeft = { null }, onRight = { it })!!.value
        val arbeidssoekerId6_2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr6_1.identitet))
            .fold(onLeft = { null }, onRight = { it })!!.value
        val arbId6_1 = arbeidssoekerId6_1.asIdentitet(gjeldende = true)
        val arbId6_2 = arbeidssoekerId6_2.asIdentitet(gjeldende = true)
        val aktorId7_1 = TestData.aktorId7_1
        val aktorId7_2 = TestData.aktorId7_2
        val dnr7 = TestData.dnr7
        val fnr7_1 = TestData.fnr7_1
        val fnr7_2 = TestData.fnr7_2
        val arbeidssoekerId7_1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr7.identitet))
            .fold(onLeft = { null }, onRight = { it })!!.value
        val arbeidssoekerId7_2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr7_1.identitet))
            .fold(onLeft = { null }, onRight = { it })!!.value
        val arbId7_1 = arbeidssoekerId7_1.asIdentitet(gjeldende = true)
        val arbId7_2 = arbeidssoekerId7_2.asIdentitet(gjeldende = true)
        val producerRecordSlot = slot<ProducerRecord<Long, IdentitetHendelse>>()
        val recordMetadata = RecordMetadata(
            TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0),
            1, 0, 0, 0, 0
        )

        beforeSpec {
            clearAllMocks()
            every {
                pawIdentitetProducerMock.sendBlocking(capture(producerRecordSlot))
            } returns recordMetadata
        }
        afterSpec {
            confirmVerified(pawIdentitetProducerMock)
            tearDown()
        }
        beforeTest { producerRecordSlot.clear() }

        "Test suite for ukjente identiteter" - {
            "Skal ikke opprette identiteter for person som ikke er arbeidssøker" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId1.identitet,
                    identiteter = listOf(
                        aktorId1,
                        dnr1.copy(gjeldende = false),
                        fnr1_1.copy(gjeldende = false),
                        fnr1_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for endringer av identiteter" - {
            "Skal opprette identiteter" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId2.identitet,
                    identiteter = listOf(
                        aktorId2,
                        dnr2.copy(gjeldende = false),
                        fnr2_1.copy(gjeldende = false),
                        fnr2_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(
                    aktorId2,
                    dnr2.copy(gjeldende = false),
                    fnr2_1.copy(gjeldende = false),
                    fnr2_2
                )
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId2.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = aktorId2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = dnr2.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_2,
                        status = IdentitetStatus.AKTIV
                    )
                )
                identitetRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId2,
                        dnr2.copy(gjeldende = false),
                        fnr2_1.copy(gjeldende = false),
                        fnr2_2,
                        arbId2
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal endre identiteter" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId2.identitet,
                    identiteter = listOf(aktorId2, dnr2),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId2.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = aktorId2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = dnr2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_1.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    )
                )
                identitetRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(aktorId2, dnr2, arbId2)
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId2,
                        dnr2.copy(gjeldende = false),
                        fnr2_1.copy(gjeldende = false),
                        fnr2_2,
                        arbId2
                    )
                }
            }

            "Skal endre ideniteter og gjenopprette slettede" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId2.identitet,
                    identiteter = listOf(aktorId2, dnr2.copy(gjeldende = false), fnr2_1),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2.copy(gjeldende = false), fnr2_1)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId2.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = aktorId2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = dnr2.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    )
                )
                identitetRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId2,
                        dnr2.copy(gjeldende = false),
                        fnr2_1,
                        arbId2
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId2, dnr2, arbId2)
                }
            }

            "Skal markere identiteter med SPLITT for ny aktør-id" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId3.identitet,
                    identiteter = listOf(
                        aktorId3,
                        dnr3.copy(gjeldende = false),
                        fnr3_1.copy(gjeldende = false),
                        fnr2_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2.copy(gjeldende = false), fnr2_1)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId2.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = aktorId2,
                        status = IdentitetStatus.SPLITT
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = dnr2.copy(gjeldende = false),
                        status = IdentitetStatus.SPLITT
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_1,
                        status = IdentitetStatus.SPLITT
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    )
                )
                identitetRepository.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                val konfliktRows = konfliktRepository.findByAktorId(aktorId3.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId3.identitet,
                    type = KonfliktType.SPLITT,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId3,
                        dnr3.copy(gjeldende = false),
                        fnr3_1.copy(gjeldende = false),
                        fnr2_2
                    )
                )
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe false
            }

            "Skal slette identiteter" {
                // WHEN
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId2.identitet,
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId2.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = aktorId2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = dnr2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_1.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId2.identitet,
                        identitet = fnr2_2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    )
                )
                konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                val konfliktRows = konfliktRepository.findByAktorId(aktorId3.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId3.identitet,
                    type = KonfliktType.SPLITT,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId3,
                        dnr3.copy(gjeldende = false),
                        fnr3_1.copy(gjeldende = false),
                        fnr2_2
                    )
                )
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterSlettetHendelse> { hendelse ->
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId2,
                        dnr2.copy(gjeldende = false),
                        fnr2_1,
                        arbId2
                    )
                }
            }
        }

        "Test suit for merge av identiteter" - {
            "Skal opprette identiteter på første arbeidssøker-id" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId6.identitet,
                    identiteter = listOf(aktorId6, dnr6),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter = identitetService.finnForAktorId(aktorId6.identitet)
                identiteter shouldContainOnly listOf(aktorId6, dnr6)
                val identitetRows = identitetRepository.findByAktorId(aktorId6.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6.identitet,
                        identitet = aktorId6,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                konfliktRepository.findByAktorId(aktorId6.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId6_1
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId6,
                        dnr6,
                        arbId6_1
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal opprette merge-konflikt" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId6.identitet,
                    identiteter = listOf(aktorId6, fnr6_1),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter = identitetService.finnForAktorId(aktorId6.identitet)
                identiteter shouldContainOnly listOf(aktorId6, dnr6)
                val identitetRows = identitetRepository.findByAktorId(aktorId6.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6.identitet,
                        identitet = aktorId6,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.MERGE
                    )
                )
                val konfliktRows = konfliktRepository.findByAktorId(aktorId6.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId6.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId6,
                        fnr6_1,
                    )
                )
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe false
            }
        }

        "Test suite for merge så slett av identiteter" - {

            "Skal opprette identiteter på første aktør-id" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId7_1.identitet,
                    identiteter = listOf(aktorId7_1, dnr7),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter shouldContainOnly listOf(aktorId7_1, dnr7)
                identitetService.finnForAktorId(aktorId7_2.identitet) shouldHaveSize 0
                val identitetRows = identitetRepository.findByAktorId(aktorId7_1.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = aktorId7_1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = dnr7,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                identitetRepository.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId7_1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId7_1
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId7_1,
                        dnr7,
                        arbId7_1
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal opprette identiteter på andre aktør-id" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId7_2.identitet,
                    identiteter = listOf(aktorId7_2, fnr7_1),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = identitetRepository.findByAktorId(aktorId7_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = aktorId7_1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = dnr7,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                val identitetRows2 = identitetRepository.findByAktorId(aktorId7_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = aktorId7_2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = fnr7_1,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                konfliktRepository.findByAktorId(aktorId7_1.identitet) shouldHaveSize 0
                konfliktRepository.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId7_2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId7_2,
                        fnr7_1,
                        arbId7_2
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal endre identiteter med MERGE av de to aktør-id'ene" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId7_1.identitet,
                    identiteter = listOf(
                        aktorId7_1,
                        aktorId7_2.copy(gjeldende = false),
                        dnr7.copy(gjeldende = false),
                        fnr7_1.copy(gjeldende = false),
                        fnr7_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = identitetRepository.findByAktorId(aktorId7_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = aktorId7_1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = dnr7,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val identitetRows2 = identitetRepository.findByAktorId(aktorId7_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = aktorId7_2,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = fnr7_1,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val konfliktRows = konfliktRepository.findByAktorId(aktorId7_1.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId7_1.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId7_1,
                        aktorId7_2.copy(gjeldende = false),
                        dnr7.copy(gjeldende = false),
                        fnr7_1.copy(gjeldende = false),
                        fnr7_2
                    )
                )
                konfliktRepository.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe false
            }

            "Skal slette identiteter for første aktør-id" {
                // WHEN
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId7_1.identitet,
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = identitetRepository.findByAktorId(aktorId7_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = aktorId7_1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_1,
                        aktorId = aktorId7_1.identitet,
                        identitet = dnr7,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val identitetRows2 = identitetRepository.findByAktorId(aktorId7_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = aktorId7_2,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId7_2,
                        aktorId = aktorId7_2.identitet,
                        identitet = fnr7_1,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val konfliktRows = konfliktRepository.findByAktorId(aktorId7_1.identitet)
                konfliktRows shouldHaveSize 2
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId7_1.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId7_1,
                        aktorId7_2.copy(gjeldende = false),
                        dnr7.copy(gjeldende = false),
                        fnr7_1.copy(gjeldende = false),
                        fnr7_2
                    )
                )
                val konfliktRow2 = konfliktRows[1]
                konfliktRow2.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId7_1.identitet,
                    type = KonfliktType.SLETT,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf()
                )
                konfliktRepository.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe false
            }
        }
    }
})