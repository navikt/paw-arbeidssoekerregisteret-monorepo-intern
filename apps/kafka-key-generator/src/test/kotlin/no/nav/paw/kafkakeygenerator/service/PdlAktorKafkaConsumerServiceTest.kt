package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asRecords
import no.nav.paw.kafkakeygenerator.test.TestData.asWrapper
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Instant

@Ignored
class PdlAktorKafkaConsumerServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val aktorTopic = applicationConfig.pdlAktorConsumer.topic

        beforeSpec {
            setUp()
            pdlAktorKafkaHwmOperations.initHwm(aktorTopic, 1)
        }

        afterSpec {
            tearDown()
        }

        "Skal ignorere meldinger for personer som ikke er arbeidssøker" {
            // GIVEN
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 1, TestData.aktorId1, TestData.aktor1_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 2, TestData.aktorId1, TestData.aktor1_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val identiteter = identitetRepository.findByAktorId(TestData.aktorId1)
            identiteter shouldHaveSize 0
            val konflikter = identitetKonfliktRepository.findByAktorId(TestData.aktorId1)
            konflikter shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.dnr1)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.fnr1_1)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.aktorId1)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.npId1)) shouldBe null
            val hwm = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm.offset shouldBe 2
        }

        "Skal ignorere meldinger med offset som ikke er over HWM" {
            // GIVEN
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 3, TestData.aktorId2, TestData.aktor2_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 4, TestData.aktorId2, TestData.aktor2_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaHwmOperations.updateHwm(aktorTopic, 0, 4, Instant.now())
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val identiteter = identitetRepository.findByAktorId(TestData.aktorId2)
            identiteter shouldHaveSize 0
            val konflikter = identitetKonfliktRepository.findByAktorId(TestData.aktorId2)
            konflikter shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.dnr2)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.fnr2_1)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.aktorId2)) shouldBe null
            kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.npId2)) shouldBe null
            val hwm = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm.offset shouldBe 4
        }

        "Skal lagre aktive ideniteter for melding med dnr så med fnr for arbeidssøker" {
            // GIVEN
            val dnr = TestData.dnr3
            val fnr = TestData.fnr3_1
            val aktorId = TestData.aktorId3
            val npId = TestData.npId3
            val arbeidssoekerId = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 5, aktorId, TestData.aktor3_1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val identiteter1 = identitetRepository.findByAktorId(aktorId)
            identiteter1 shouldHaveSize 3
            identiteter1.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.AKTIV
                )
            )
            val konflikter1 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter1 shouldHaveSize 0
            val hendelser1 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser1 shouldHaveSize 1
            val kdnr1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr1 shouldNotBe null
            arbeidssoekerId shouldBe kdnr1?.arbeidssoekerId
            val kfnr1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr1 shouldBe null
            val kaktorId1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId1 shouldBe null
            val knpId1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId1 shouldBe null
            val hwm1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm1.offset shouldBe 5

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 6, aktorId, TestData.aktor3_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val identiteter2 = identitetRepository.findByAktorId(aktorId)
            identiteter2 shouldHaveSize 4
            identiteter2.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    false,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    fnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.AKTIV
                )
            )
            val konflikter2 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter2 shouldHaveSize 0
            val hendelser2 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser2 shouldHaveSize 2
            val kdnr2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr2 shouldNotBe null
            arbeidssoekerId shouldBe kdnr2?.arbeidssoekerId
            val kfnr2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr2 shouldBe null
            val kaktorId2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId2 shouldBe null
            val knpId2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId2 shouldBe null
            val hwm2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm2.offset shouldBe 6

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 7, aktorId, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            val identiteter3 = identitetRepository.findByAktorId(aktorId)
            identiteter3 shouldHaveSize 4
            identiteter3.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    false,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    fnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.SLETTET
                )
            )
            val konflikter3 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter3 shouldHaveSize 0
            val hendelser3 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser3 shouldHaveSize 3
            val kdnr3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr3 shouldNotBe null
            arbeidssoekerId shouldBe kdnr3?.arbeidssoekerId
            val kfnr3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr3 shouldBe null
            val kaktorId3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId3 shouldBe null
            val knpId3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId3 shouldBe null
            val hwm3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm3.offset shouldBe 7
        }

        "Skal lagre identiteter med konflikt for melding med dnr så for fnr for arbeidssøker med to arbeidssøkerIder" {
            // GIVEN
            val dnr = TestData.dnr4
            val fnr = TestData.fnr4_1
            val aktorId = TestData.aktorId4
            val npId = TestData.npId4
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 8, aktorId, TestData.aktor4_1)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
            val identiteter1 = identitetRepository.findByAktorId(aktorId)
            identiteter1 shouldHaveSize 3
            identiteter1.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.AKTIV
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.AKTIV
                )
            )
            val konflikter1 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter1 shouldHaveSize 0
            val hendelser1 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser1 shouldHaveSize 1
            val kdnr1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr1 shouldNotBe null
            arbeidssoekerId1 shouldBe kdnr1?.arbeidssoekerId
            val kfnr1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr1 shouldNotBe null
            arbeidssoekerId2 shouldBe kfnr1?.arbeidssoekerId
            val kaktorId1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId1 shouldBe null
            val knpId1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId1 shouldBe null
            val hwm1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm1.offset shouldBe 8

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 9, aktorId, TestData.aktor4_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val identiteter2 = identitetRepository.findByAktorId(aktorId)
            identiteter2 shouldHaveSize 4
            identiteter2.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    false,
                    IdentitetStatus.KONFLIKT
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId2,
                    aktorId,
                    fnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.KONFLIKT
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.KONFLIKT
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.KONFLIKT
                )
            )
            val konflikter2 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter2 shouldHaveSize 1
            val hendelser2 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser2 shouldHaveSize 1
            val kdnr2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr2 shouldNotBe null
            arbeidssoekerId1 shouldBe kdnr2?.arbeidssoekerId
            val kfnr2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr2 shouldNotBe null
            arbeidssoekerId2 shouldBe kfnr2?.arbeidssoekerId
            val kaktorId2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId2 shouldBe null
            val knpId2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId2 shouldBe null
            val hwm2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm2.offset shouldBe 9

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 10, aktorId, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            val identiteter3 = identitetRepository.findByAktorId(aktorId)
            identiteter3 shouldHaveSize 4
            identiteter3.map { it.asWrapper() } shouldContainOnly listOf(
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    dnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    false,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId2,
                    aktorId,
                    fnr,
                    IdentitetType.FOLKEREGISTERIDENT,
                    true,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    aktorId,
                    IdentitetType.AKTORID,
                    true,
                    IdentitetStatus.SLETTET
                ),
                TestData.IdentitetWrapper(
                    arbeidssoekerId1,
                    aktorId,
                    npId,
                    IdentitetType.NPID,
                    true,
                    IdentitetStatus.SLETTET
                )
            )
            val konflikter3 = identitetKonfliktRepository.findByAktorId(aktorId)
            konflikter3 shouldHaveSize 1
            val hendelser3 = identitetHendelseRepository.findByAktorId(aktorId)
            hendelser3 shouldHaveSize 1
            val kdnr3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(dnr))
            kdnr3 shouldNotBe null
            arbeidssoekerId1 shouldBe kdnr3?.arbeidssoekerId
            val kfnr3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(fnr))
            kfnr3 shouldNotBe null
            arbeidssoekerId2 shouldBe kfnr3?.arbeidssoekerId
            val kaktorId3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(aktorId))
            kaktorId3 shouldBe null
            val knpId3 = kafkaKeysIdentitetRepository.find(Identitetsnummer(npId))
            knpId3 shouldBe null
            val hwm3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwm3.offset shouldBe 10
        }
    }
})