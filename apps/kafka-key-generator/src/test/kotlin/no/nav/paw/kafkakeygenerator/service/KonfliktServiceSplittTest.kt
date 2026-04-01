package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSplittetHendelse
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestDatabase
import no.nav.paw.kafkakeygenerator.test.asWrapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant

class KonfliktServiceSplittTest : FreeSpec({
    mockkStatic("no.nav.paw.kafka.producer.ProducerUtilsKt")
    with(TestContext.buildWithPostgres()) {
        val identitetRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)

        beforeEach { clearAllMocks() }
        beforeSpec { setUp() }
        afterSpec { tearDown() }

        "Skal pause splitt-konflikt med flere arbeidssoeker-ider" {
            // GIVEN
            val aktorId1 = TestData.aktorId7_1
            val aktorId2 = TestData.aktorId7_2
            val dnr = TestData.dnr7_1
            val fnr1 = TestData.fnr7_1
            val fnr2 = TestData.fnr7_2
            val arbeidssoekerId1 = KafkaKeysTable.insert().value
            val arbeidssoekerId2 = KafkaKeysTable.insert().value
            val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

            every {
                pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
            } returns identitetRecordMetadata

            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId1.identitet,
                identitet = aktorId1.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId1.identitet,
                identitet = dnr.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId2.identitet,
                identitet = aktorId2.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId2.identitet,
                identitet = fnr1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )

            KonflikterTable.insert(
                aktorId = aktorId2.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.VENTER,
                sourceTimestamp = Instant.now(),
                identiteter = listOf(
                    aktorId1.copy(gjeldende = false),
                    aktorId2,
                    dnr.copy(gjeldende = false),
                    fnr1.copy(gjeldende = false),
                    fnr2
                )
            )

            // WHEN
            konfliktService.handleSplittKonflikter()

            // THEN
            val konfliktRows1 = KonflikterTable.findByAktorId(aktorId1.identitet)
            konfliktRows1 shouldHaveSize 0
            val konfliktRows2 = KonflikterTable.findByAktorId(aktorId2.identitet)
            konfliktRows2 shouldHaveSize 1

            val konfliktRow2_1 = konfliktRows2[0]
            konfliktRow2_1.aktorId shouldBe aktorId2.identitet
            konfliktRow2_1.type shouldBe KonfliktType.SPLITT
            konfliktRow2_1.status shouldBe KonfliktStatus.PAUSET
            konfliktRow2_1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId1.copy(gjeldende = false),
                aktorId2,
                dnr.copy(gjeldende = false),
                fnr1.copy(gjeldende = false),
                fnr2
            )

            val identitetRows1 = IdentiteterTable.findByAktorId(aktorId1.identitet)
            identitetRows1 shouldHaveSize 2
            identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1.identitet,
                    identitet = aktorId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1.identitet,
                    identitet = dnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            val identitetRows2 = IdentiteterTable.findByAktorId(aktorId2.identitet)
            identitetRows2 shouldHaveSize 2
            identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId2.identitet,
                    identitet = aktorId2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId2.identitet,
                    identitet = fnr1,
                    status = IdentitetStatus.AKTIV
                )
            )

            identitetProducerRecordList shouldHaveSize 0
        }

        "Skal fullføre splitt-konflikt med samme aktor-id og arbeidssoeker-id" {
            // GIVEN
            val aktorId = TestData.aktorId8_1
            val dnr = TestData.dnr8_1
            val fnr1 = TestData.fnr8_1
            val fnr2 = TestData.fnr8_2
            val arbeidssoekerId = KafkaKeysTable.insert().value
            val arbId = arbeidssoekerId.asIdentitet()
            val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

            every {
                pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
            } returns identitetRecordMetadata

            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = aktorId.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = dnr.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = fnr1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.SLETTET,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )

            KonflikterTable.insert(
                aktorId = aktorId.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.VENTER,
                sourceTimestamp = Instant.now(),
                identiteter = listOf(aktorId, fnr1.copy(gjeldende = false), fnr2)
            )

            // WHEN
            konfliktService.handleSplittKonflikter()

            // THEN
            val konfliktRows2 = KonflikterTable.findByAktorId(aktorId.identitet)
            konfliktRows2 shouldHaveSize 1

            val konfliktRow2_1 = konfliktRows2[0]
            konfliktRow2_1.aktorId shouldBe aktorId.identitet
            konfliktRow2_1.type shouldBe KonfliktType.SPLITT
            konfliktRow2_1.status shouldBe KonfliktStatus.FULLFOERT
            konfliktRow2_1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId,
                fnr1.copy(gjeldende = false),
                fnr2
            )

            val identitetRows2 = IdentiteterTable.findByAktorId(aktorId.identitet)
            identitetRows2 shouldHaveSize 4
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
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
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

            identitetProducerRecordList shouldHaveSize 1
            val identitetRecord = identitetProducerRecordList[0]
            identitetRecord.key() shouldBe arbeidssoekerId
            identitetRecord.value().shouldBeInstanceOf<IdentiteterSplittetHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(
                    aktorId,
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

        "Skal fullføre splitt-konflikt med foskjellig aktor-id og samme arbeidssoeker-id" {
            // GIVEN
            val aktorId1 = TestData.aktorId9_1
            val aktorId2 = TestData.aktorId9_2
            val dnr = TestData.dnr9_1
            val fnr1 = TestData.fnr9_1
            val fnr2 = TestData.fnr9_2
            val arbeidssoekerId = KafkaKeysTable.insert().value
            val arbId = arbeidssoekerId.asIdentitet()
            val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

            every {
                pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
            } returns identitetRecordMetadata

            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId1.identitet,
                identitet = aktorId1.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId1.identitet,
                identitet = dnr.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId1.identitet,
                identitet = fnr1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.SLETTET,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )

            KonflikterTable.insert(
                aktorId = aktorId2.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.VENTER,
                sourceTimestamp = Instant.now(),
                identiteter = listOf(aktorId1.copy(gjeldende = false), aktorId2, fnr1.copy(gjeldende = false), fnr2)
            )

            // WHEN
            konfliktService.handleSplittKonflikter()

            // THEN
            val konfliktRows1 = KonflikterTable.findByAktorId(aktorId1.identitet)
            konfliktRows1 shouldHaveSize 0
            val konfliktRows2 = KonflikterTable.findByAktorId(aktorId2.identitet)
            konfliktRows2 shouldHaveSize 1

            val konfliktRow2_1 = konfliktRows2[0]
            konfliktRow2_1.aktorId shouldBe aktorId2.identitet
            konfliktRow2_1.type shouldBe KonfliktType.SPLITT
            konfliktRow2_1.status shouldBe KonfliktStatus.FULLFOERT
            konfliktRow2_1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId1.copy(gjeldende = false),
                aktorId2,
                fnr1.copy(gjeldende = false),
                fnr2
            )

            val identitetRows1 = IdentiteterTable.findByAktorId(aktorId1.identitet)
            identitetRows1 shouldHaveSize 0
            val identitetRows2 = IdentiteterTable.findByAktorId(aktorId2.identitet)
            identitetRows2 shouldHaveSize 5
            identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = aktorId1.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = aktorId2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = fnr1.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = fnr2,
                    status = IdentitetStatus.AKTIV
                )
            )

            identitetProducerRecordList shouldHaveSize 1
            val identitetRecord = identitetProducerRecordList[0]
            identitetRecord.key() shouldBe arbeidssoekerId
            identitetRecord.value().shouldBeInstanceOf<IdentiteterSplittetHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(
                    aktorId1.copy(gjeldende = false),
                    aktorId2,
                    fnr1.copy(gjeldende = false),
                    fnr2,
                    arbId
                )
                hendelse.tidligereIdentiteter shouldContainOnly listOf(
                    aktorId1,
                    dnr,
                    arbId
                )
            }
        }

        "Skal sette pausede splitt-konflikter til venter" {
            // GIVEN
            val aktorId1_1 = TestData.aktorId10_1
            val aktorId1_2 = TestData.aktorId10_2
            val aktorId2_1 = TestData.aktorId11_1
            val aktorId2_2 = TestData.aktorId11_2
            val aktorId3_1 = TestData.aktorId12_1
            val aktorId3_2 = TestData.aktorId12_2
            val dnr1 = TestData.dnr10_1
            val dnr2 = TestData.dnr11_1
            val dnr3 = TestData.dnr12_1
            val fnr1_1 = TestData.fnr10_1
            val fnr1_2 = TestData.fnr10_2
            val fnr2_1 = TestData.fnr11_1
            val fnr2_2 = TestData.fnr11_2
            val fnr3_1 = TestData.fnr12_1
            val fnr3_2 = TestData.fnr12_2
            val arbeidssoekerId1 = KafkaKeysTable.insert().value
            val arbeidssoekerId2 = KafkaKeysTable.insert().value
            val arbeidssoekerId3_1 = KafkaKeysTable.insert().value
            val arbeidssoekerId3_2 = KafkaKeysTable.insert().value
            val arbId1 = arbeidssoekerId1.asIdentitet()
            val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()

            every {
                pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
            } returns identitetRecordMetadata

            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId1_1.identitet,
                identitet = aktorId1_1.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId1_1.identitet,
                identitet = dnr1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId1_1.identitet,
                identitet = fnr1_1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId2_1.identitet,
                identitet = aktorId2_1.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId2_1.identitet,
                identitet = dnr2.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId2_1.identitet,
                identitet = fnr2_1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId3_1,
                aktorId = aktorId3_1.identitet,
                identitet = aktorId3_1.identitet,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId3_1,
                aktorId = aktorId3_1.identitet,
                identitet = dnr3.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            IdentiteterTable.insert(
                arbeidssoekerId = arbeidssoekerId3_2,
                aktorId = aktorId3_1.identitet,
                identitet = fnr3_1.identitet,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.AKTIV,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )

            TestDatabase.insertKonflikt(
                aktorId = aktorId1_2.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.PAUSET,
                sourceTimestamp = Instant.now().minus(Duration.ofHours(26)),
                insertedTimestamp = Instant.now().minus(Duration.ofHours(26)),
                updatedTimestamp = Instant.now().minus(Duration.ofHours(25)),
                identiteter = listOf(
                    aktorId1_1.copy(gjeldende = false),
                    aktorId1_2,
                    fnr1_1.copy(gjeldende = false),
                    fnr1_2
                )
            )
            TestDatabase.insertKonflikt(
                aktorId = aktorId2_2.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.PAUSET,
                sourceTimestamp = Instant.now().minus(Duration.ofHours(22)),
                insertedTimestamp = Instant.now().minus(Duration.ofHours(22)),
                updatedTimestamp = Instant.now().minus(Duration.ofHours(23)),
                identiteter = listOf(
                    aktorId2_1.copy(gjeldende = false),
                    aktorId2_2,
                    fnr2_1.copy(gjeldende = false),
                    fnr2_2
                )
            )
            TestDatabase.insertKonflikt(
                aktorId = aktorId3_2.identitet,
                type = KonfliktType.SPLITT,
                status = KonfliktStatus.PAUSET,
                sourceTimestamp = Instant.now().minus(Duration.ofHours(32)),
                insertedTimestamp = Instant.now().minus(Duration.ofHours(32)),
                updatedTimestamp = Instant.now().minus(Duration.ofHours(33)),
                identiteter = listOf(
                    aktorId3_1.copy(gjeldende = false),
                    aktorId3_2,
                    dnr3.copy(gjeldende = false),
                    fnr3_1.copy(gjeldende = false),
                    fnr3_2
                )
            )

            // WHEN
            konfliktService.handleSplittKonflikter()

            // THEN
            val konfliktRows1_1 = KonflikterTable.findByAktorId(aktorId1_1.identitet)
            konfliktRows1_1 shouldHaveSize 0
            val konfliktRows1_2 = KonflikterTable.findByAktorId(aktorId1_2.identitet)
            konfliktRows1_2 shouldHaveSize 1
            val konfliktRows2_1 = KonflikterTable.findByAktorId(aktorId2_1.identitet)
            konfliktRows2_1 shouldHaveSize 0
            val konfliktRows2_2 = KonflikterTable.findByAktorId(aktorId2_2.identitet)
            konfliktRows2_2 shouldHaveSize 1
            val konfliktRows3_1 = KonflikterTable.findByAktorId(aktorId3_1.identitet)
            konfliktRows3_1 shouldHaveSize 0
            val konfliktRows3_2 = KonflikterTable.findByAktorId(aktorId3_2.identitet)
            konfliktRows3_2 shouldHaveSize 1

            val konfliktRow1_2 = konfliktRows1_2[0]
            konfliktRow1_2.aktorId shouldBe aktorId1_2.identitet
            konfliktRow1_2.type shouldBe KonfliktType.SPLITT
            konfliktRow1_2.status shouldBe KonfliktStatus.FULLFOERT
            konfliktRow1_2.identiteter shouldHaveSize 4
            konfliktRow1_2.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId1_1.copy(gjeldende = false),
                aktorId1_2,
                fnr1_1.copy(gjeldende = false),
                fnr1_2,
            )
            val konfliktRow2_2 = konfliktRows2_2[0]
            konfliktRow2_2.aktorId shouldBe aktorId2_2.identitet
            konfliktRow2_2.type shouldBe KonfliktType.SPLITT
            konfliktRow2_2.status shouldBe KonfliktStatus.PAUSET
            konfliktRow2_2.identiteter shouldHaveSize 4
            konfliktRow2_2.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId2_1.copy(gjeldende = false),
                aktorId2_2,
                fnr2_1.copy(gjeldende = false),
                fnr2_2
            )
            val konfliktRow3_2 = konfliktRows3_2[0]
            konfliktRow3_2.aktorId shouldBe aktorId3_2.identitet
            konfliktRow3_2.type shouldBe KonfliktType.SPLITT
            konfliktRow3_2.status shouldBe KonfliktStatus.PAUSET
            konfliktRow3_2.identiteter shouldHaveSize 5
            konfliktRow3_2.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                aktorId3_1.copy(gjeldende = false),
                aktorId3_2,
                dnr3.copy(gjeldende = false),
                fnr3_1.copy(gjeldende = false),
                fnr3_2
            )

            val identitetRows1_1 = IdentiteterTable.findByAktorId(aktorId1_1.identitet)
            identitetRows1_1 shouldHaveSize 0
            val identitetRows1_2 = IdentiteterTable.findByAktorId(aktorId1_2.identitet)
            identitetRows1_2 shouldHaveSize 5
            val identitetRows2_1 = IdentiteterTable.findByAktorId(aktorId2_1.identitet)
            identitetRows2_1 shouldHaveSize 3
            val identitetRows2_2 = IdentiteterTable.findByAktorId(aktorId2_2.identitet)
            identitetRows2_2 shouldHaveSize 0
            val identitetRows3_1 = IdentiteterTable.findByAktorId(aktorId3_1.identitet)
            identitetRows3_1 shouldHaveSize 3
            val identitetRows3_2 = IdentiteterTable.findByAktorId(aktorId3_2.identitet)
            identitetRows3_2 shouldHaveSize 0

            identitetRows1_2.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1_2.identitet,
                    identitet = aktorId1_1.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1_2.identitet,
                    identitet = aktorId1_2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1_2.identitet,
                    identitet = dnr1.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1_2.identitet,
                    identitet = fnr1_1.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId1_2.identitet,
                    identitet = fnr1_2,
                    status = IdentitetStatus.AKTIV
                )
            )
            identitetRows2_1.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId2_1.identitet,
                    identitet = aktorId2_1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId2_1.identitet,
                    identitet = dnr2.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId2_1.identitet,
                    identitet = fnr2_1,
                    status = IdentitetStatus.AKTIV
                )
            )
            identitetRows3_1.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId3_1,
                    aktorId = aktorId3_1.identitet,
                    identitet = aktorId3_1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId3_1,
                    aktorId = aktorId3_1.identitet,
                    identitet = dnr3.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId3_2,
                    aktorId = aktorId3_1.identitet,
                    identitet = fnr3_1,
                    status = IdentitetStatus.AKTIV
                )
            )

            identitetProducerRecordList shouldHaveSize 1
            val identitetRecord = identitetProducerRecordList[0]
            identitetRecord.key() shouldBe arbeidssoekerId1
            identitetRecord.value().shouldBeInstanceOf<IdentiteterSplittetHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(
                    aktorId1_1.copy(gjeldende = false),
                    aktorId1_2,
                    fnr1_1.copy(gjeldende = false),
                    fnr1_2,
                    arbId1
                )
                hendelse.tidligereIdentiteter shouldContainOnly listOf(
                    aktorId1_1,
                    dnr1.copy(gjeldende = false),
                    fnr1_1,
                    arbId1
                )
            }
        }
    }
})