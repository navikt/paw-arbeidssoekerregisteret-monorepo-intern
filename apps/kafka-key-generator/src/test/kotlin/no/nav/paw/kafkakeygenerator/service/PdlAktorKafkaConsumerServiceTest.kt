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
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.KonfliktWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.TestData.asRecords
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.time.Instant

class PdlAktorKafkaConsumerServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val aktorTopic = applicationConfig.pdlAktorConsumer.topic
        val producerRecordSlot = slot<ProducerRecord<Long, IdentitetHendelse>>()
        val recordMetadata =
            RecordMetadata(TopicPartition(applicationConfig.pawIdentitetProducer.topic, 0), 1, 0, 0, 0, 0)

        beforeSpec {
            clearAllMocks()
            setUp()
            pdlAktorKafkaHwmOperations.initHwm(aktorTopic, 1)
            every {
                pawIdentitetProducerMock.sendBlocking(capture(producerRecordSlot))
            } returns recordMetadata
        }

        afterSpec {
            confirmVerified(
                pawIdentitetProducerMock,
                pawHendelseloggProducerMock
            )
            tearDown()
        }

        afterTest {
            verify { pawHendelseloggProducerMock wasNot Called }
        }

        "Skal ignorere meldinger for personer som ikke er arbeidssøker" {
            // GIVEN
            val aktorId = TestData.aktorId1
            val npId = TestData.npId1
            val dnr = TestData.dnr1
            val fnr = TestData.fnr1_1

            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 1, aktorId.identitet, TestData.aktor1_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 2, aktorId.identitet, TestData.aktor1_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            identitetService.finnForAktorId(aktorId.identitet) shouldHaveSize 0
            identitetRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 2
            verify { pawIdentitetProducerMock wasNot Called }
        }

        "Skal ignorere meldinger med offset som ikke er over HWM" {
            // GIVEN
            val aktorId = TestData.aktorId2
            val npId = TestData.npId2
            val dnr = TestData.dnr2
            val fnr = TestData.fnr2_1

            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 3, aktorId.identitet, TestData.aktor2_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 4, aktorId.identitet, TestData.aktor2_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaHwmOperations.updateHwm(aktorTopic, 0, 4, Instant.now())
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            identitetService.finnForAktorId(aktorId.identitet) shouldHaveSize 0
            identitetRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 4
            verify { pawIdentitetProducerMock wasNot Called }
        }

        /**
         * melding 1: aktorId -> dnr(gjeldende)
         * melding 2: aktorId -> dnr, fnr1, fnr2(gjeldende)
         * melding 3: aktorId -> dnr, fnr2(gjeldende)
         * melding 4: aktorId -> null(tombstone)
         */
        "Skal lagre endring på identiteter for arbeidssøker" {
            // GIVEN
            val aktorId = TestData.aktorId3
            val npId = TestData.npId3
            val dnr = TestData.dnr3
            val fnr1 = TestData.fnr3_1
            val fnr2 = TestData.fnr3_2
            val arbeidssoekerId = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val aktor1 = TestData.aktor3_1
            val aktor2 = TestData.aktor3_2
            val aktor3 = TestData.aktor3_3

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 5, aktorId.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val ideniteter1 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter1 shouldContainOnly listOf(aktorId, npId, dnr)
            val identitetRows1 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows1 shouldHaveSize 3
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
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 5
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 6, aktorId.identitet, aktor2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val ideniteter2 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter2 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr.copy(gjeldende = false),
                fnr1.copy(gjeldende = false),
                fnr2
            )
            val identitetRows2 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows2 shouldHaveSize 5
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
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 6
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
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

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 7, aktorId.identitet, aktor3)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            val ideniteter3 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter3 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr.copy(gjeldende = false),
                fnr2
            )
            val identitetRows3 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows3 shouldHaveSize 5
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
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 7
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr2, arbId)
                hendelse.tidligereIdentiteter shouldContainOnly listOf(
                    aktorId,
                    npId,
                    dnr.copy(gjeldende = false),
                    fnr1.copy(gjeldende = false),
                    fnr2,
                    arbId
                )
            }

            // GIVEN
            val records4: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 8, aktorId.identitet, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records4)

            // THEN
            val ideniteter4 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter4 shouldBe emptyList()
            val identitetRows4 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows4 shouldHaveSize 5
            identitetRows4.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = npId.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
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
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow4 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow4.offset shouldBe 8
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterSlettetHendelse> { hendelse ->
                hendelse.tidligereIdentiteter shouldContainOnly listOf(
                    aktorId,
                    npId,
                    dnr.copy(gjeldende = false),
                    fnr2,
                    arbId
                )
            }
        }

        /**
         * melding 1: aktorId -> dnr(gjeldende)
         * melding 2: aktorId -> dnr, fnr1(gjeldende)
         * melding 3: aktorId -> dnr, fnr2(gjeldende)
         * melding 4: aktorId -> null(tombstone)
         */
        "Skal lagre merge-konflikt for melding med dnr så for fnr for arbeidssøker med to arbeidssøkerIder" {
            // GIVEN
            val aktorId = TestData.aktorId4
            val npId = TestData.npId4
            val dnr = TestData.dnr4
            val fnr1 = TestData.fnr4_1
            val fnr2 = TestData.fnr4_2
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr1.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId1.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val aktor1 = TestData.aktor4_1
            val aktor2 = TestData.aktor4_2
            val aktor3 = TestData.aktor4_3

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 9, aktorId.identitet, aktor1)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val ideniteter1 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter1 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr
            )
            val identitetRows1 = identitetRepository.findByAktorId(aktorId.identitet)
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
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr1.identitet)
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 9
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId1
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId, npId, dnr, arbId1)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 10, aktorId.identitet, aktor2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val ideniteter2 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter2 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr
            )
            val identitetRows2 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows2 shouldHaveSize 3
            identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
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
                )
            )
            val konfliktRows2 = konfliktRepository.findByAktorId(aktorId.identitet)
            konfliktRows2 shouldHaveSize 1
            konfliktRows2.map { it.asWrapper() } shouldContainOnly listOf(
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr1)
                )
            )
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr1.identitet)
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 10

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 11, aktorId.identitet, aktor3)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            val ideniteter3 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter3 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr
            )
            val identitetRows3 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows3 shouldHaveSize 3
            identitetRows3.map { it.asWrapper() } shouldContainOnly listOf(
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
                )
            )
            val konfliktRows3 = konfliktRepository.findByAktorId(aktorId.identitet)
            konfliktRows3 shouldHaveSize 1
            konfliktRows3.map { it.asWrapper() } shouldContainOnly listOf(
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr2)
                )
            )
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow5 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow5 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow6 = kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer())
            kafkaKeyRow6 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr1.identitet)
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 11

            // GIVEN
            val records4: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 12, aktorId.identitet, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records4)

            // THEN
            val ideniteter4 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter4 shouldContainOnly listOf(
                aktorId,
                npId,
                dnr
            )
            val identitetRows4 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows4 shouldHaveSize 3
            identitetRows4.map { it.asWrapper() } shouldContainOnly listOf(
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
                )
            )
            val konfliktRows4 = konfliktRepository.findByAktorId(aktorId.identitet)
            konfliktRows4 shouldHaveSize 2
            konfliktRows4.map { it.asWrapper() } shouldContainOnly listOf(
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr2)
                ),
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.SLETT,
                    status = KonfliktStatus.VENTER,
                    identiteter = emptyList()
                )
            )
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val kafkaKeyRow7 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow7 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow8 = kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer())
            kafkaKeyRow8 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr1.identitet)
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow4 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow4.offset shouldBe 12
        }

        /**
         * melding 1: aktorId1 -> dnr, fnr(gjeldende)
         * melding 2: aktorId2 -> fnr(gjeldende)
         */
        "Skal lagre splitt-konflikt for melding med fnr på ny aktørId for arbeidssøker" {
            // GIVEN
            val aktorId1 = TestData.aktorId7_1
            val aktorId2 = TestData.aktorId7_2
            val npId = TestData.npId7
            val dnr = TestData.dnr7
            val fnr = TestData.fnr7_1
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val aktor1 = TestData.aktor7_1
            val aktor2 = TestData.aktor7_2

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 13, aktorId1.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val ideniteter1 = identitetService.finnForAktorId(aktorId1.identitet)
            ideniteter1 shouldContainOnly listOf(
                aktorId1,
                npId,
                dnr.copy(gjeldende = false),
                fnr
            )
            identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
            val identitetRows1 = identitetRepository.findByAktorId(aktorId1.identitet)
            identitetRows1 shouldHaveSize 4
            identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = aktorId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = npId,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = fnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            identitetRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 13
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId1, npId, dnr.copy(gjeldende = false), fnr, arbId1)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 14, aktorId2.identitet, aktor2),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val ideniteter2 = identitetService.finnForAktorId(aktorId1.identitet)
            ideniteter2 shouldContainOnly listOf(
                aktorId1,
                npId,
                dnr.copy(gjeldende = false),
                fnr
            )
            identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
            val identitetRows2 = identitetRepository.findByAktorId(aktorId1.identitet)
            identitetRows2 shouldHaveSize 4
            identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = aktorId1,
                    status = IdentitetStatus.SPLITT
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = npId,
                    status = IdentitetStatus.SPLITT
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.SPLITT
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = fnr,
                    status = IdentitetStatus.SPLITT
                )
            )
            identitetRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            val konfliktRows = konfliktRepository.findByAktorId(aktorId2.identitet)
            konfliktRows shouldHaveSize 1
            konfliktRows.map { it.asWrapper() } shouldContainOnly listOf(
                KonfliktWrapper(
                    aktorId = aktorId2.identitet,
                    type = KonfliktType.SPLITT,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(aktorId2, fnr)
                )
            )
            hendelseRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 14
        }

        "Skal lagre endring på identiteter med ny aktørId for arbeidssøker" {
            /**
             * melding 1: aktorId1 -> dnr(gjeldende)
             * melding 2: aktorId2 -> fnr(gjeldende)
             * melding 3: aktorId2 -> dnr, fnr(gjeldende)
             */
            // GIVEN
            val aktorId1 = TestData.aktorId8_1
            val aktorId2 = TestData.aktorId8_2
            val npId1 = TestData.npId8_1
            val npId2 = TestData.npId8_2
            val dnr = TestData.dnr8
            val fnr = TestData.fnr8
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            kafkaKeysRepository.lagre(fnr.asIdentitetsnummer(), ArbeidssoekerId(arbeidssoekerId))
            val aktor1 = TestData.aktor8_1
            val aktor2 = TestData.aktor8_2
            val aktor3 = TestData.aktor8_3

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 15, aktorId1.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val ideniteter1 = identitetService.finnForAktorId(aktorId1.identitet)
            ideniteter1 shouldContainOnly listOf(
                aktorId1,
                npId1,
                dnr
            )
            identitetService.finnForAktorId(aktorId2.identitet) shouldHaveSize 0
            val identitetRows1 = identitetRepository.findByAktorId(aktorId1.identitet)
            identitetRows1 shouldHaveSize 3
            identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = aktorId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = npId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = dnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            identitetRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 15
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId1, npId1, dnr, arbId1)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 16, aktorId2.identitet, aktor2),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val ideniteter2 = identitetService.finnForAktorId(aktorId1.identitet)
            ideniteter2 shouldContainOnly listOf(
                aktorId1,
                npId1,
                dnr
            )
            val ideniteter3 = identitetService.finnForAktorId(aktorId2.identitet)
            ideniteter3 shouldContainOnly listOf(
                aktorId2,
                npId2,
                fnr
            )
            val identitetRows2 = identitetRepository.findByAktorId(aktorId1.identitet)
            identitetRows2 shouldHaveSize 3
            identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = aktorId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = npId1,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId1.identitet,
                    identitet = dnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            val identitetRows3 = identitetRepository.findByAktorId(aktorId2.identitet)
            identitetRows3 shouldHaveSize 3
            identitetRows3.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = aktorId2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = npId2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = fnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 16
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId2, npId2, fnr, arbId1)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 17, aktorId2.identitet, aktor3),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            identitetService.finnForAktorId(aktorId1.identitet) shouldHaveSize 0
            val ideniteter4 = identitetService.finnForAktorId(aktorId2.identitet)
            ideniteter4 shouldContainOnly listOf(
                aktorId1.copy(gjeldende = false),
                aktorId2,
                npId1.copy(gjeldende = false),
                npId2,
                dnr.copy(gjeldende = false),
                fnr
            )
            identitetRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            val identitetRows4 = identitetRepository.findByAktorId(aktorId2.identitet)
            identitetRows4 shouldHaveSize 6
            identitetRows4.map { it.asWrapper() } shouldContainOnly listOf(
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
                    identitet = npId1.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = npId2,
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.AKTIV
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId2.identitet,
                    identitet = fnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            konfliktRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId1.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow5 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow5 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow6 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow6 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 17
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(
                    aktorId1.copy(gjeldende = false),
                    aktorId2,
                    npId1.copy(gjeldende = false),
                    npId2,
                    dnr.copy(gjeldende = false),
                    fnr,
                    arbId1
                )
                hendelse.tidligereIdentiteter shouldContainOnly listOf(aktorId2, npId2, fnr, arbId1)
            }
        }

        "Skal slette identiteter for tombstone-melding" {
            // GIVEN
            val aktorId = TestData.aktorId9
            val npId = TestData.npId9
            val dnr = TestData.dnr9
            val fnr = TestData.fnr9
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)

            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = dnr.identitet,
                type = dnr.type,
                status = IdentitetStatus.AKTIV,
                gjeldende = false,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = fnr.identitet,
                type = fnr.type,
                status = IdentitetStatus.AKTIV,
                gjeldende = true,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = aktorId.identitet,
                type = aktorId.type,
                status = IdentitetStatus.AKTIV,
                gjeldende = true,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                identitet = npId.identitet,
                type = npId.type,
                status = IdentitetStatus.AKTIV,
                gjeldende = true,
                sourceTimestamp = Instant.now()
            )
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 18, aktorId.identitet, null),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val ideniteter = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter shouldBe emptyList()
            val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows shouldHaveSize 4
            identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = npId.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
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
                    identitet = fnr.copy(gjeldende = false),
                    status = IdentitetStatus.SLETTET
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 18
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterSlettetHendelse> { hendelse ->
                hendelse.tidligereIdentiteter shouldContainOnly listOf(
                    aktorId,
                    npId,
                    dnr.copy(gjeldende = false),
                    fnr,
                    arbId1
                )
            }
        }

        "Skal ignorere duplikate meldinger" {
            // GIVEN
            val aktorId = TestData.aktorId10
            val npId = TestData.npId10
            val fnr = TestData.fnr10
            val arbeidssoekerId = kafkaKeysRepository.opprett(fnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 19, aktorId.identitet, TestData.aktor10),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val ideniteter1 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter1 shouldContainOnly listOf(
                aktorId,
                npId,
                fnr
            )
            val identitetRows1 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows1 shouldHaveSize 3
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
                    identitet = fnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 19
            verify { pawIdentitetProducerMock.sendBlocking(any<ProducerRecord<Long, IdentitetHendelse>>()) }
            producerRecordSlot.isCaptured shouldBe true
            producerRecordSlot.captured.key() shouldBe arbeidssoekerId
            producerRecordSlot.captured.value().shouldBeInstanceOf<IdentiteterEndretHendelse> { hendelse ->
                hendelse.identiteter shouldContainOnly listOf(aktorId, npId, fnr, arbId1)
                hendelse.tidligereIdentiteter shouldBe emptyList()
            }

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 20, aktorId.identitet, TestData.aktor10),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val ideniteter2 = identitetService.finnForAktorId(aktorId.identitet)
            ideniteter2 shouldContainOnly listOf(
                aktorId,
                npId,
                fnr
            )
            val identitetRows2 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows2 shouldHaveSize 3
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
                    identitet = fnr,
                    status = IdentitetStatus.AKTIV
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 20
        }
    }
})