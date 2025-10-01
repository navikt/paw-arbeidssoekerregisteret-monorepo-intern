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
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.test.insert
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

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
                val aktorId = TestData.aktorId1
                val npId = TestData.npId1
                val dnr = TestData.dnr1
                val fnr1 = TestData.fnr1_1
                val fnr2 = TestData.fnr1_2
                val arbeidssoekerId1 = kafkaKeysRepository.insert(dnr)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(fnr1)
                val arbeidssoekerId3 = kafkaKeysRepository.insert(fnr2)
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
                    hendelse.identitetsnummer shouldBe aktorId.identitet
                    hendelse.flyttedeIdentitetsnumre shouldContainOnly setOf(
                        aktorId.identitet,
                        npId.identitet,
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
                val aktorId = TestData.aktorId2
                val npId = TestData.npId2
                val dnr = TestData.dnr2
                val fnr1 = TestData.fnr2_1
                val fnr2 = TestData.fnr2_2
                val periodeId1 = TestData.periodeId2_1
                val periodeId2 = TestData.periodeId2_2
                val periodeId3 = TestData.periodeId2_3
                val arbeidssoekerId1 = kafkaKeysRepository.insert(dnr)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(fnr1)
                val arbeidssoekerId3 = kafkaKeysRepository.insert(fnr2)
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
                val aktorId = TestData.aktorId3
                val npId = TestData.npId3
                val dnr = TestData.dnr3
                val fnr1 = TestData.fnr3_1
                val fnr2 = TestData.fnr3_2
                val periodeId1 = TestData.periodeId3_1
                val periodeId2 = TestData.periodeId3_2
                val periodeId3 = TestData.periodeId3_3
                val arbeidssoekerId1 = kafkaKeysRepository.insert(dnr)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(fnr1)
                val arbeidssoekerId3 = kafkaKeysRepository.insert(fnr2)
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
                val aktorId = TestData.aktorId4
                val npId = TestData.npId4
                val dnr = TestData.dnr4
                val fnr1 = TestData.fnr4_1
                val fnr2 = TestData.fnr4_2
                val periodeId1 = TestData.periodeId4_1
                val periodeId2 = TestData.periodeId4_2
                val periodeId3 = TestData.periodeId4_3
                val arbeidssoekerId1 = kafkaKeysRepository.insert(dnr)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(fnr1)
                val arbeidssoekerId3 = kafkaKeysRepository.insert(fnr2)
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

                verify { pawIdentitetProducerMock wasNot Called }
                verify { pawHendelseloggProducerMock wasNot Called }
            }

            "Skal håndtere merge-konflikt uten lagrede identiteter" {
                // GIVEN
                val aktorId = TestData.aktorId5
                val npId = TestData.npId5
                val dnr = TestData.dnr5
                val fnr1 = TestData.fnr5_1
                val fnr2 = TestData.fnr5_2
                val arbeidssoekerId1 = kafkaKeysRepository.insert(aktorId)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(dnr)
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

            "Skal håndtere merge-konflikt med bytting av identiteter" {
                // GIVEN
                val aktorId = TestData.aktorId6
                val dnr = TestData.dnr6
                val fnr = TestData.fnr6_1
                val arbeidssoekerId1 = kafkaKeysRepository.insert(dnr)
                val arbeidssoekerId2 = kafkaKeysRepository.insert(fnr)
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
                    identitet = dnr.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )

                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, fnr)
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
                    aktorId, fnr
                )

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 3
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.SLETTET
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val kafkaKeyRows = kafkaKeysIdentitetRepository
                    .findByIdentiteter(
                        identiteter = setOf(
                            aktorId.identitet, dnr.identitet, fnr.identitet
                        )
                    )
                kafkaKeyRows shouldHaveSize 2
                kafkaKeyRows shouldContainOnly listOf(
                    KafkaKeyRow(arbeidssoekerId1, dnr.identitet),
                    KafkaKeyRow(arbeidssoekerId2, fnr.identitet),
                )

                verify(exactly = 2) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                identitetProducerRecordList shouldHaveSize 2
                val identitetRecord1 = identitetProducerRecordList[0]
                val identitetRecord2 = identitetProducerRecordList[1]
                identitetRecord1.key() shouldBe arbeidssoekerId1
                identitetRecord1.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldBe emptyList()
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        aktorId,
                        dnr,
                        arbId1
                    )
                }
                identitetRecord2.key() shouldBe arbeidssoekerId2
                identitetRecord2.value().shouldBeInstanceOf<IdentiteterMergetHendelse> { hendelse ->
                    hendelse.identiteter shouldContainOnly listOf(
                        aktorId,
                        fnr,
                        arbId1.copy(gjeldende = false),
                        arbId2
                    )
                    hendelse.tidligereIdentiteter shouldContainOnly listOf(
                        arbId2
                    )
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
                    hendelse.identitetsnummer shouldBe fnr.identitet
                    hendelse.kilde.arbeidssoekerId shouldBe arbeidssoekerId1
                    hendelse.kilde.identitetsnummer shouldContainOnly setOf(
                        aktorId.identitet
                    )
                }
            }

            "Skal håndtere mange merge-konflikter" {
                // GIVEN
                val localDate = LocalDate.of(2000, 1, 1)
                val identiteter = (1L..10L).map { i ->
                    val fnrD = localDate.dayOfMonth.toString().padStart(2, '0')
                    val dnrD = (localDate.dayOfMonth + 40).toString().padStart(2, '0')
                    val m = localDate.monthValue.toString().padStart(2, '0')
                    val y = (localDate.year - 2000).toString().padStart(2, '0')
                    val del2 = i.toString().padStart(5, '0')
                    val fnr = Identitet(
                        identitet = "$fnrD$m$y$del2",
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true
                    )
                    val dnr = Identitet(
                        identitet = "$dnrD$m$y$del2",
                        type = IdentitetType.FOLKEREGISTERIDENT,
                        gjeldende = true
                    )
                    val aktorId = Identitet(
                        identitet = "20000$fnrD$m$y$del2",
                        type = IdentitetType.AKTORID,
                        gjeldende = true
                    )
                    localDate.plusDays(i)
                    Triple(aktorId, dnr, fnr)
                }

                val lagredeIdentiteter = identiteter.map { i ->
                    val (aktorId, dnr, fnr) = i
                    val arbeidssoekerId1 = kafkaKeysRepository.insert(aktorId)
                    val arbeidssoekerId2 = kafkaKeysRepository.insert(dnr)
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
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = dnr.identitet,
                        type = dnr.type,
                        gjeldende = dnr.gjeldende,
                        status = IdentitetStatus.MERGE,
                        sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                    )

                    konfliktRepository.insert(
                        aktorId = aktorId.identitet,
                        type = KonfliktType.MERGE,
                        status = KonfliktStatus.VENTER,
                        sourceTimestamp = Instant.now(),
                        identiteter = listOf(aktorId, dnr.copy(gjeldende = false), fnr)
                    )

                    Pair(i, Pair(arbeidssoekerId1, arbeidssoekerId2))
                }

                // THEN
                lagredeIdentiteter.forEach { i ->
                    val (i1, i2) = i
                    val (aktorId, dnr, fnr) = i1
                    val (arbeidssoekerId1, arbeidssoekerId2) = i2
                    val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                    konfliktRows shouldHaveSize 1

                    val konfliktRow1 = konfliktRows[0]
                    konfliktRow1.aktorId shouldBe aktorId.identitet
                    konfliktRow1.type shouldBe KonfliktType.MERGE
                    konfliktRow1.status shouldBe KonfliktStatus.VENTER
                    konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                        aktorId,
                        dnr.copy(gjeldende = false),
                        fnr
                    )

                    val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                    identitetRows shouldHaveSize 2
                    identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId1,
                            aktorId = aktorId.identitet,
                            identitet = aktorId,
                            status = IdentitetStatus.MERGE
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId2,
                            aktorId = aktorId.identitet,
                            identitet = dnr,
                            status = IdentitetStatus.MERGE
                        )
                    )
                }

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                lagredeIdentiteter.forEach { i ->
                    val (i1, i2) = i
                    val (aktorId, dnr, fnr) = i1
                    val (arbeidssoekerId1, arbeidssoekerId2) = i2
                    val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                    konfliktRows shouldHaveSize 1

                    val konfliktRow1 = konfliktRows[0]
                    konfliktRow1.aktorId shouldBe aktorId.identitet
                    konfliktRow1.type shouldBe KonfliktType.MERGE
                    konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                    konfliktRow1.identiteter.map { it.asIdentitet() } shouldContainOnly listOf(
                        aktorId,
                        dnr.copy(gjeldende = false),
                        fnr
                    )

                    val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                    identitetRows shouldHaveSize 3
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
                            identitet = dnr.copy(gjeldende = false),
                            status = IdentitetStatus.AKTIV
                        ),
                        IdentitetWrapper(
                            arbeidssoekerId = arbeidssoekerId2,
                            aktorId = aktorId.identitet,
                            identitet = fnr,
                            status = IdentitetStatus.AKTIV
                        )
                    )
                }

                verify(exactly = 20) { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
                verify(exactly = 20) { pawHendelseloggProducerMock.sendBlocking(any<ProducerRecord<Long, Hendelse>>()) }
            }
        }
    }
})