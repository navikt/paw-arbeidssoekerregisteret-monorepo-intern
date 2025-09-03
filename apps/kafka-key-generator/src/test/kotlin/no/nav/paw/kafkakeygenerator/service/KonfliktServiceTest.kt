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
import io.mockk.verify
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant

class KonfliktServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val identitetRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)
        val hendelsesloggRecordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawHendelseloggProducer.topic, 0), 1, 0, 0, 0, 0)

        beforeEach { clearAllMocks() }
        beforeSpec { setUp() }
        afterTest {
            confirmVerified(
                pawIdentitetProducerMock,
                pawHendelseloggProducerMock
            )
        }
        afterSpec { tearDown() }

        /**
         * ----- MERGE -----
         */
        "Tester for merge-konflikter" - {
            "Skal håndtere merge-konflikt uten perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId1, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId1, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr1_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr1_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr1.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(fnr2.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)
                val recordKey1 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId1)).value
                val recordKey2 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId2)).value
                val recordKey3 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId3)).value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()
                val hendelseloggProducerRecordList = mutableListOf<ProducerRecord<Long, Hendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata
                every {
                    pawHendelseloggProducerMock.sendBlocking(capture(hendelseloggProducerRecordList))
                } returns hendelsesloggRecordMetadata

                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, fnr1.copy(gjeldende = false), fnr2)
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1

                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                    aktorId, npId, fnr1.copy(gjeldende = false), fnr2
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, npId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 3
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, dnr.identitet),
                    KafkaKeyRow(arbeidssoekerId2, fnr1.identitet),
                    KafkaKeyRow(arbeidssoekerId3, fnr2.identitet)
                )

                hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0

                verify(exactly = 3) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 3
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                val identitetRecord3 = identitetProducerRecordList[2]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                }
                identitetRecord2.key() shouldBe arbeidssoekerId2
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr1, arbId2)
                }
                identitetRecord3.key() shouldBe arbeidssoekerId3
                identitetRecord3.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        fnr1.copy(gjeldende = false),
                        fnr2,
                        arbId1.copy(gjeldende = false),
                        arbId2.copy(gjeldende = false),
                        arbId3
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr2, arbId3)
                }

                verify(exactly = 4) { pawHendelseloggProducerMock.sendBlocking(any<ProducerRecord<Long, Hendelse>>()) }
                hendelseloggProducerRecordList shouldHaveSize 4
                val hendelseloggRecord1 = hendelseloggProducerRecordList[0]
                val hendelseloggRecord2 = hendelseloggProducerRecordList[1]
                val hendelseloggRecord3 = hendelseloggProducerRecordList[2]
                val hendelseloggRecord4 = hendelseloggProducerRecordList[3]
                hendelseloggRecord1.key() shouldBe recordKey1
                hendelseloggRecord1.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId1
                    hendelse.identitetsnummer shouldBe dnr.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        aktorId.identitet,
                        npId.identitet,
                        dnr.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId3
                }
                hendelseloggRecord2.key() shouldBe recordKey3
                hendelseloggRecord2.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId3
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId1
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        aktorId.identitet,
                        npId.identitet,
                        dnr.identitet
                    )
                }
                hendelseloggRecord3.key() shouldBe recordKey2
                hendelseloggRecord3.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId2
                    hendelse.identitetsnummer shouldBe fnr1.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        fnr1.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId3
                }
                hendelseloggRecord4.key() shouldBe recordKey3
                hendelseloggRecord4.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId3
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId2
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        fnr1.identitet
                    )
                }
            }

            "Skal håndtere merge-konflikt med kun avsluttede perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId2, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId2, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr2, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr2_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr2_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId2_1
                val periodeId2 = TestData.periodeId2_2
                val periodeId3 = TestData.periodeId2_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr1.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(fnr2.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)
                val recordKey1 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId1)).value
                val recordKey2 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId2)).value
                val recordKey3 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId3)).value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()
                val hendelseloggProducerRecordList = mutableListOf<ProducerRecord<Long, Hendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata
                every {
                    pawHendelseloggProducerMock.sendBlocking(capture(hendelseloggProducerRecordList))
                } returns hendelsesloggRecordMetadata

                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2)
                )
                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(90)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                    aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
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

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, npId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 3
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, dnr.identitet),
                    KafkaKeyRow(arbeidssoekerId2, fnr1.identitet),
                    KafkaKeyRow(arbeidssoekerId3, fnr2.identitet)
                )

                hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0

                verify(exactly = 3) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 3
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                val identitetRecord3 = identitetProducerRecordList[2]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1.copy(gjeldende = false),
                        fnr2,
                        arbId1,
                        arbId2.copy(gjeldende = false),
                        arbId3.copy(gjeldende = false)
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                }
                identitetRecord2.key() shouldBe arbeidssoekerId2
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr1, arbId2)
                }
                identitetRecord3.key() shouldBe arbeidssoekerId3
                identitetRecord3.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr2, arbId3)
                }

                verify(exactly = 4) { pawHendelseloggProducerMock.sendBlocking(any<ProducerRecord<Long, Hendelse>>()) }
                hendelseloggProducerRecordList shouldHaveSize 4
                val hendelseloggRecord1 = hendelseloggProducerRecordList[0]
                val hendelseloggRecord2 = hendelseloggProducerRecordList[1]
                val hendelseloggRecord3 = hendelseloggProducerRecordList[2]
                val hendelseloggRecord4 = hendelseloggProducerRecordList[3]
                hendelseloggRecord1.key() shouldBe recordKey2
                hendelseloggRecord1.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId2
                    hendelse.identitetsnummer shouldBe fnr1.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        fnr1.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId1
                }
                hendelseloggRecord2.key() shouldBe recordKey1
                hendelseloggRecord2.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId1
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId2
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        fnr1.identitet
                    )
                }
                hendelseloggRecord3.key() shouldBe recordKey3
                hendelseloggRecord3.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId3
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        fnr2.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId1
                }
                hendelseloggRecord4.key() shouldBe recordKey1
                hendelseloggRecord4.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId1
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId3
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        fnr2.identitet
                    )
                }
            }

            "Skal håndtere merge-konflikt med én aktiv periode" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId3, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId3, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr3, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr3_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr3_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId3_1
                val periodeId2 = TestData.periodeId3_2
                val periodeId3 = TestData.periodeId3_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)
                val recordKey1 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId1)).value
                val recordKey2 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId2)).value
                val recordKey3 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId3)).value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()
                val hendelseloggProducerRecordList = mutableListOf<ProducerRecord<Long, Hendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata
                every {
                    pawHendelseloggProducerMock.sendBlocking(capture(hendelseloggProducerRecordList))
                } returns hendelsesloggRecordMetadata

                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr1, fnr2.copy(gjeldende = false))
                )
                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows.first()
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                    aktorId, npId, dnr.copy(gjeldende = false), fnr1, fnr2.copy(gjeldende = false)
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr2.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    )
                )

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, npId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 3
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, dnr.identitet),
                    KafkaKeyRow(arbeidssoekerId2, fnr1.identitet),
                    KafkaKeyRow(arbeidssoekerId3, fnr2.identitet)
                )

                hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0

                verify(exactly = 3) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 3
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                val identitetRecord3 = identitetProducerRecordList[2]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                }
                identitetRecord2.key() shouldBe arbeidssoekerId2
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1,
                        fnr2.copy(gjeldende = false),
                        arbId1.copy(gjeldende = false),
                        arbId2,
                        arbId3.copy(gjeldende = false)
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr1, arbId2)
                }
                identitetRecord3.key() shouldBe arbeidssoekerId3
                identitetRecord3.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(fnr2, arbId3)
                }

                verify(exactly = 4) { pawHendelseloggProducerMock.sendBlocking(any<ProducerRecord<Long, Hendelse>>()) }
                hendelseloggProducerRecordList shouldHaveSize 4
                val hendelseloggRecord1 = hendelseloggProducerRecordList[0]
                val hendelseloggRecord2 = hendelseloggProducerRecordList[1]
                val hendelseloggRecord3 = hendelseloggProducerRecordList[2]
                val hendelseloggRecord4 = hendelseloggProducerRecordList[3]
                hendelseloggRecord1.key() shouldBe recordKey1
                hendelseloggRecord1.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId1
                    hendelse.identitetsnummer shouldBe dnr.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        aktorId.identitet,
                        npId.identitet,
                        dnr.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId2
                }
                hendelseloggRecord2.key() shouldBe recordKey2
                hendelseloggRecord2.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId2
                    hendelse.identitetsnummer shouldBe fnr1.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId1
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        aktorId.identitet,
                        npId.identitet,
                        dnr.identitet
                    )
                }
                hendelseloggRecord3.key() shouldBe recordKey3
                hendelseloggRecord3.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId3
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        fnr2.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId2
                }
                hendelseloggRecord4.key() shouldBe recordKey2
                hendelseloggRecord4.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId2
                    hendelse.identitetsnummer shouldBe fnr1.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId3
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        fnr2.identitet
                    )
                }
            }

            "Skal håndtere merge-konflikt med to aktive perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId4, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId4, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr4, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr4_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr4_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId4_1
                val periodeId2 = TestData.periodeId4_2
                val periodeId3 = TestData.periodeId4_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()
                val hendelseloggProducerRecordList = mutableListOf<ProducerRecord<Long, Hendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata
                every {
                    pawHendelseloggProducerMock.sendBlocking(capture(hendelseloggProducerRecordList))
                } returns hendelsesloggRecordMetadata

                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = IdentitetType.AKTORID,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = IdentitetType.NPID,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )

                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2)
                )

                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows.first()
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FEILET
                konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                    aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.MERGE
                    )
                )

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, npId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 3
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, dnr.identitet),
                    KafkaKeyRow(arbeidssoekerId2, fnr1.identitet),
                    KafkaKeyRow(arbeidssoekerId3, fnr2.identitet)
                )

                hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0

                verify { pawIdentitetProducerMock wasNot Called }
                verify { pawHendelseloggProducerMock wasNot Called }
            }

            "Skal håndtere merge-konflikt uten lagrede identiteter" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId5, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId5, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr5, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr1 = Identitet(TestData.fnr5_1, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr2 = Identitet(TestData.fnr5_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(aktorId.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val recordKey1 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId1)).value
                val recordKey2 = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId2)).value
                val identitetProducerRecordList = mutableListOf<ProducerRecord<Long, IdentitetHendelse>>()
                val hendelseloggProducerRecordList = mutableListOf<ProducerRecord<Long, Hendelse>>()

                every {
                    pawIdentitetProducerMock.sendBlocking(capture(identitetProducerRecordList))
                } returns identitetRecordMetadata
                every {
                    pawHendelseloggProducerMock.sendBlocking(capture(hendelseloggProducerRecordList))
                } returns hendelsesloggRecordMetadata

                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2)
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1

                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                    aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, npId.identitet, dnr.identitet, fnr1.identitet, fnr2.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 2
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, aktorId.identitet),
                    KafkaKeyRow(arbeidssoekerId2, dnr.identitet),
                )

                hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0

                verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 2
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(arbId1)
                }
                identitetRecord2.key() shouldBe arbeidssoekerId2
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        npId,
                        dnr.copy(gjeldende = false),
                        fnr1.copy(gjeldende = false),
                        fnr2,
                        arbId1.copy(gjeldende = false),
                        arbId2,
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(arbId2)
                }

                verify(exactly = 2) { pawHendelseloggProducerMock.sendBlocking(any<ProducerRecord<Long, Hendelse>>()) }
                hendelseloggProducerRecordList shouldHaveSize 2
                val hendelseloggRecord1 = hendelseloggProducerRecordList[0]
                val hendelseloggRecord2 = hendelseloggProducerRecordList[1]
                hendelseloggRecord1.key() shouldBe recordKey1
                hendelseloggRecord1.value().shouldBeInstanceOf<IdentitetsnummerSammenslaatt> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId1
                    hendelse.identitetsnummer shouldBe aktorId.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        aktorId.identitet
                    )
                    hendelse.flyttetTilArbeidssoekerId shouldBe arbeidssoekerId2
                }
                hendelseloggRecord2.key() shouldBe recordKey2
                hendelseloggRecord2.value().shouldBeInstanceOf<ArbeidssoekerIdFlettetInn> { hendelse ->
                    hendelse.id shouldBe arbeidssoekerId2
                    hendelse.identitetsnummer shouldBe fnr2.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId1
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        aktorId.identitet
                    )
                }
            }
        }
    }
})