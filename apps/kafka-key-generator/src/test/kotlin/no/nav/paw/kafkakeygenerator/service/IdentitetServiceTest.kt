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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSlettetHendelse
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.KonfliktWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Instant

@ExperimentalCoroutinesApi
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
        val aktorId3 = TestData.aktorId3
        val dnr3 = TestData.dnr3
        val fnr3_1 = TestData.fnr3_1
        val aktorId6_1 = TestData.aktorId6_1
        val aktorId6_2 = TestData.aktorId6_2
        val dnr6 = TestData.dnr6
        val fnr6_1 = TestData.fnr6_1
        val fnr6_2 = TestData.fnr6_2
        val aktorId7_1 = TestData.aktorId7_1
        val aktorId7_2 = TestData.aktorId7_2
        val dnr7 = TestData.dnr7
        val fnr7_1 = TestData.fnr7_1
        val fnr7_2 = TestData.fnr7_2
        val aktorId9 = TestData.aktorId9
        val dnr9 = TestData.dnr9
        val fnr9_1 = TestData.fnr9_1
        val fnr9_2 = TestData.fnr9_2
        val producerRecordSlot = slot<ProducerRecord<Long, IdentitetHendelse>>()
        val recordMetadata = RecordMetadata(
            TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0),
            1, 0, 0, 0, 0
        )

        afterSpec {
            tearDown()
        }
        beforeTest {
            producerRecordSlot.clear()
            clearAllMocks()
            every {
                pawIdentitetProducerMock.sendBlocking(capture(producerRecordSlot))
            } returns recordMetadata
        }
        afterTest {
            confirmVerified(pawIdentitetProducerMock)
        }

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
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for endringer av identiteter" - {
            "Skal opprette identiteter" {
                // WHEN
                identitetService.identiteterSkalOpprettes(
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
                val arbeidssoekerId2 = TestData.hentArbeidssoekerId(aktorId2.identitet)
                val arbId2 = arbeidssoekerId2.asIdentitet()

                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(
                    aktorId2,
                    dnr2.copy(gjeldende = false),
                    fnr2_1.copy(gjeldende = false),
                    fnr2_2
                )
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId2.identitet)
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
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
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
                val arbeidssoekerId2 = TestData.hentArbeidssoekerId(aktorId2.identitet)
                val arbId2 = arbeidssoekerId2.asIdentitet()

                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId2.identitet)
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
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
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
                val arbeidssoekerId2 = TestData.hentArbeidssoekerId(aktorId2.identitet)
                val arbId2 = arbeidssoekerId2.asIdentitet()

                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2.copy(gjeldende = false), fnr2_1)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId2.identitet)
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
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
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
                val arbeidssoekerId2 = TestData.hentArbeidssoekerId(aktorId2.identitet)

                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                val identiteter = identitetService.finnForAktorId(aktorId2.identitet)
                identiteter shouldContainOnly listOf(aktorId2, dnr2.copy(gjeldende = false), fnr2_1)
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                IdentiteterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId2.identitet)
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
                IdentiteterTable.findByAktorId(aktorId3.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                val konfliktRows = KonflikterTable.findByAktorId(aktorId3.identitet)
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
                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal slette identiteter" {
                // WHEN
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId2.identitet,
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId2 = TestData.hentArbeidssoekerId(aktorId2.identitet)
                val arbId2 = arbeidssoekerId2.asIdentitet()

                identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
                identitetService.finnForAktorId(aktorId3.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId2.identitet)
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
                KonflikterTable.findByAktorId(aktorId1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId2.identitet) shouldHaveSize 0
                val konfliktRows = KonflikterTable.findByAktorId(aktorId3.identitet)
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

        "Test suit for merge så endring av identiteter" - {
            "Skal opprette identiteter på første aktør-id" {
                // WHEN
                identitetService.identiteterSkalOpprettes(
                    aktorId = aktorId6_1.identitet,
                    identiteter = listOf(aktorId6_1, dnr6),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId6_1 = TestData.hentArbeidssoekerId(aktorId6_1.identitet)
                val arbId6_1 = arbeidssoekerId6_1.asIdentitet()

                val identiteter = identitetService.finnForAktorId(aktorId6_1.identitet)
                identiteter shouldContainOnly listOf(aktorId6_1, dnr6)
                identitetService.finnForAktorId(aktorId6_2.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId6_1.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = aktorId6_1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                IdentiteterTable.findByAktorId(aktorId6_2.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId6_1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId6_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId6_1
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId6_1,
                        dnr6,
                        arbId6_1
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal opprette identiteter på andre aktør-id" {
                // WHEN
                identitetService.identiteterSkalOpprettes(
                    aktorId = aktorId6_2.identitet,
                    identiteter = listOf(aktorId6_2, fnr6_1),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId6_1 = TestData.hentArbeidssoekerId(aktorId6_1.identitet)
                val arbeidssoekerId6_2 = TestData.hentArbeidssoekerId(aktorId6_2.identitet)
                val arbId6_2 = arbeidssoekerId6_2.asIdentitet()

                val identiteter1 = identitetService.finnForAktorId(aktorId6_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId6_1, dnr6)
                val identiteter2 = identitetService.finnForAktorId(aktorId6_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId6_2, fnr6_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId6_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = aktorId6_1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId6_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = aktorId6_2,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = fnr6_1,
                        status = IdentitetStatus.AKTIV
                    ),
                )
                KonflikterTable.findByAktorId(aktorId6_1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId6_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId6_2
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId6_2,
                        fnr6_1,
                        arbId6_2
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal opprette merge-konflikt" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId6_2.identitet,
                    identiteter = listOf(
                        aktorId6_1.copy(gjeldende = false),
                        aktorId6_2,
                        dnr6.copy(gjeldende = false),
                        fnr6_1
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId6_1 = TestData.hentArbeidssoekerId(aktorId6_1.identitet)
                val arbeidssoekerId6_2 = TestData.hentArbeidssoekerId(aktorId6_2.identitet)

                val identiteter1 = identitetService.finnForAktorId(aktorId6_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId6_1, dnr6)
                val identiteter2 = identitetService.finnForAktorId(aktorId6_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId6_2, fnr6_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId6_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = aktorId6_1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId6_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = aktorId6_2,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = fnr6_1,
                        status = IdentitetStatus.MERGE
                    ),
                )
                KonflikterTable.findByAktorId(aktorId6_1.identitet) shouldHaveSize 0
                val konfliktRows2 = KonflikterTable.findByAktorId(aktorId6_2.identitet)
                konfliktRows2 shouldHaveSize 1
                val konfliktRow1 = konfliktRows2[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId6_2.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId6_1.copy(gjeldende = false),
                        aktorId6_2,
                        dnr6.copy(gjeldende = false),
                        fnr6_1
                    )
                )
                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal endre identiteter med MERGE av de to aktør-id'ene" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId6_2.identitet,
                    identiteter = listOf(
                        aktorId6_1.copy(gjeldende = false),
                        aktorId6_2,
                        dnr6.copy(gjeldende = false),
                        fnr6_1.copy(gjeldende = false),
                        fnr6_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId6_1 = TestData.hentArbeidssoekerId(aktorId6_1.identitet)
                val arbeidssoekerId6_2 = TestData.hentArbeidssoekerId(aktorId6_2.identitet)

                val identiteter1 = identitetService.finnForAktorId(aktorId6_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId6_1, dnr6)
                val identiteter2 = identitetService.finnForAktorId(aktorId6_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId6_2, fnr6_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId6_1.identitet)
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = aktorId6_1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_1,
                        aktorId = aktorId6_1.identitet,
                        identitet = dnr6,
                        status = IdentitetStatus.MERGE
                    ),
                )
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId6_2.identitet)
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = aktorId6_2,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId6_2,
                        aktorId = aktorId6_2.identitet,
                        identitet = fnr6_1,
                        status = IdentitetStatus.MERGE
                    ),
                )
                KonflikterTable.findByAktorId(aktorId6_1.identitet) shouldHaveSize 0
                val konfliktRows2 = KonflikterTable.findByAktorId(aktorId6_2.identitet)
                konfliktRows2 shouldHaveSize 1
                val konfliktRow1 = konfliktRows2[0]
                konfliktRow1.asWrapper() shouldBe KonfliktWrapper(
                    aktorId = aktorId6_2.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(
                        aktorId6_1.copy(gjeldende = false),
                        aktorId6_2,
                        dnr6.copy(gjeldende = false),
                        fnr6_1.copy(gjeldende = false),
                        fnr6_2
                    )
                )
                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for merge så slett av identiteter" - {

            "Skal opprette identiteter på første aktør-id" {
                // WHEN
                identitetService.identiteterSkalOpprettes(
                    aktorId = aktorId7_1.identitet,
                    identiteter = listOf(aktorId7_1, dnr7),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId7_1 = TestData.hentArbeidssoekerId(aktorId7_1.identitet)
                val arbId7_1 = arbeidssoekerId7_1.asIdentitet()

                val identiteter = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter shouldContainOnly listOf(aktorId7_1, dnr7)
                identitetService.finnForAktorId(aktorId7_2.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId7_1.identitet)
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
                IdentiteterTable.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId7_1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
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
                identitetService.identiteterSkalOpprettes(
                    aktorId = aktorId7_2.identitet,
                    identiteter = listOf(aktorId7_2, fnr7_1),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId7_1 = TestData.hentArbeidssoekerId(aktorId7_1.identitet)
                val arbeidssoekerId7_2 = TestData.hentArbeidssoekerId(aktorId7_2.identitet)
                val arbId7_2 = arbeidssoekerId7_2.asIdentitet()

                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId7_1.identitet)
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
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId7_2.identitet)
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
                KonflikterTable.findByAktorId(aktorId7_1.identitet) shouldHaveSize 0
                KonflikterTable.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
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
                val arbeidssoekerId7_1 = TestData.hentArbeidssoekerId(aktorId7_1.identitet)
                val arbeidssoekerId7_2 = TestData.hentArbeidssoekerId(aktorId7_2.identitet)

                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId7_1.identitet)
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
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId7_2.identitet)
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
                val konfliktRows = KonflikterTable.findByAktorId(aktorId7_1.identitet)
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
                KonflikterTable.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal slette identiteter for første aktør-id" {
                // WHEN
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId7_1.identitet,
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId7_1 = TestData.hentArbeidssoekerId(aktorId7_1.identitet)
                val arbeidssoekerId7_2 = TestData.hentArbeidssoekerId(aktorId7_2.identitet)

                val identiteter1 = identitetService.finnForAktorId(aktorId7_1.identitet)
                identiteter1 shouldContainOnly listOf(aktorId7_1, dnr7)
                val identiteter2 = identitetService.finnForAktorId(aktorId7_2.identitet)
                identiteter2 shouldContainOnly listOf(aktorId7_2, fnr7_1)
                val identitetRows1 = IdentiteterTable.findByAktorId(aktorId7_1.identitet)
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
                val identitetRows2 = IdentiteterTable.findByAktorId(aktorId7_2.identitet)
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
                val konfliktRows = KonflikterTable.findByAktorId(aktorId7_1.identitet)
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
                KonflikterTable.findByAktorId(aktorId7_2.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock wasNot Called }
            }
        }

        "Test suite for duplikat så slett av identiteter" - {
            "Skal opprette identiteter" {
                // WHEN
                identitetService.identiteterSkalOpprettes(
                    aktorId = aktorId9.identitet,
                    identiteter = listOf(aktorId9, dnr9),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId9 = TestData.hentArbeidssoekerId(aktorId9.identitet)
                val arbId9 = arbeidssoekerId9.asIdentitet()

                val identiteter1 = identitetService.finnForAktorId(aktorId9.identitet)
                identiteter1 shouldContainOnly listOf(aktorId9, dnr9)
                val identitetRows = IdentiteterTable.findByAktorId(aktorId9.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = aktorId9,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = dnr9,
                        status = IdentitetStatus.AKTIV
                    )
                )
                KonflikterTable.findByAktorId(aktorId9.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId9
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId9,
                        dnr9,
                        arbId9
                    )
                    hendelse.tidligereIdentiteter shouldBe emptyList()
                }
            }

            "Skal endre identiteter" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId9.identitet,
                    identiteter = listOf(
                        aktorId9,
                        dnr9.copy(gjeldende = false),
                        fnr9_1.copy(gjeldende = false),
                        fnr9_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId9 = TestData.hentArbeidssoekerId(aktorId9.identitet)
                val arbId9 = arbeidssoekerId9.asIdentitet()

                val identiteter = identitetService.finnForAktorId(aktorId9.identitet)
                identiteter shouldContainOnly listOf(
                    aktorId9,
                    dnr9.copy(gjeldende = false),
                    fnr9_1.copy(gjeldende = false),
                    fnr9_2
                )
                val identitetRows = IdentiteterTable.findByAktorId(aktorId9.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = aktorId9,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = dnr9.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_2,
                        status = IdentitetStatus.AKTIV
                    )
                )
                KonflikterTable.findByAktorId(aktorId9.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId9
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId9,
                        dnr9.copy(gjeldende = false),
                        fnr9_1.copy(gjeldende = false),
                        fnr9_2,
                        arbId9
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId9,
                        dnr9,
                        arbId9
                    )
                }
            }

            "Skal håndtere duplikater" {
                // WHEN
                identitetService.identiteterSkalOppdateres(
                    aktorId = aktorId9.identitet,
                    identiteter = listOf(
                        aktorId9,
                        dnr9.copy(gjeldende = false),
                        fnr9_1.copy(gjeldende = false),
                        fnr9_2
                    ),
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId9 = TestData.hentArbeidssoekerId(aktorId9.identitet)

                val identiteter = identitetService.finnForAktorId(aktorId9.identitet)
                identiteter shouldContainOnly listOf(
                    aktorId9,
                    dnr9.copy(gjeldende = false),
                    fnr9_1.copy(gjeldende = false),
                    fnr9_2
                )
                val identitetRows = IdentiteterTable.findByAktorId(aktorId9.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = aktorId9,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = dnr9.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_2,
                        status = IdentitetStatus.AKTIV
                    )
                )
                KonflikterTable.findByAktorId(aktorId9.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock wasNot Called }
            }

            "Skal slette identiteter" {
                // WHEN
                identitetService.identiteterSkalSlettes(
                    aktorId = aktorId9.identitet,
                    sourceTimestamp = Instant.now(),
                )

                // THEN
                val arbeidssoekerId9 = TestData.hentArbeidssoekerId(aktorId9.identitet)
                val arbId9 = arbeidssoekerId9.asIdentitet()

                identitetService.finnForAktorId(aktorId9.identitet) shouldHaveSize 0
                val identitetRows = IdentiteterTable.findByAktorId(aktorId9.identitet)
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = aktorId9.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = dnr9.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_1.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId9,
                        aktorId = aktorId9.identitet,
                        identitet = fnr9_2.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    )
                )
                KonflikterTable.findByAktorId(aktorId9.identitet) shouldHaveSize 0
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                producerRecordSlot.isCaptured shouldBe true
                producerRecordSlot.captured.key() shouldBe arbeidssoekerId9
                producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterSlettetHendelse> { hendelse ->
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId9,
                        dnr9.copy(gjeldende = false),
                        fnr9_1.copy(gjeldende = false),
                        fnr9_2,
                        arbId9
                    )
                }
            }
        }

        "Test case for race condition" - {
            "Opprett" {
                val svar = CoroutineScope(Dispatchers.IO).async {
                    val a1 = async {
                        println("A1 start")
                        identitetService.identiteterSkalOpprettes(
                            identiteter = TestData.aktor1_1.identifikatorer.map { it.asIdentitet() }
                        )
                        println("A1 stopp")
                        1
                    }
                    val a2 = async {
                        println("A2 start")
                        identitetService.identiteterSkalOpprettes(
                            identiteter = TestData.aktor1_1.identifikatorer.map { it.asIdentitet() }
                        )
                        println("A2 stopp")
                        2
                    }
                    a1.join()
                    a2.join()
                    val aRes = a1.getCompleted()
                    val bRes = a2.getCompleted()
                    println("Resultat: ${aRes + bRes}")

                    aRes + bRes
                }
                svar.join()
                println("Yay: ${svar.getCompleted()}")
                verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            }
        }
    }
})