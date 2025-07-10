package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.identitet.internehendelser.IDENTITETER_ENDRET_HENDELSE_TYPE
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.*
import no.nav.paw.kafkakeygenerator.test.*
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.TestData.asRecords
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Instant

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
            val aktorId = Identitet(TestData.aktorId1, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId1, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr1, IdentitetType.FOLKEREGISTERIDENT, false)
            val fnr = Identitet(TestData.fnr1_1, IdentitetType.FOLKEREGISTERIDENT, true)
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 1, aktorId.identitet, TestData.aktor1_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 2, aktorId.identitet, TestData.aktor1_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            identitetRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 2
        }

        "Skal ignorere meldinger med offset som ikke er over HWM" {
            // GIVEN
            val aktorId = Identitet(TestData.aktorId2, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId2, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr2, IdentitetType.FOLKEREGISTERIDENT, true)
            val fnr = Identitet(TestData.fnr2_1, IdentitetType.FOLKEREGISTERIDENT, true)
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 3, aktorId.identitet, TestData.aktor2_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 4, aktorId.identitet, TestData.aktor2_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaHwmOperations.updateHwm(aktorTopic, 0, 4, Instant.now())
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            identitetRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            hendelseRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 4
        }

        "Skal lagre endring på ideniteter for arbeidssøker" {
            /**
             * melding 1: aktorId -> dnr(gjeldende)
             * melding 2: aktorId -> dnr, fnr1, fnr2(gjeldende)
             * melding 3: aktorId -> dnr, fnr2(gjeldende)
             * melding 4: aktorId -> null(tombstone)
             */
            val aktorId = Identitet(TestData.aktorId3, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId3, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr3, IdentitetType.FOLKEREGISTERIDENT, true)
            val fnr1 = Identitet(TestData.fnr3_1, IdentitetType.FOLKEREGISTERIDENT, true)
            val fnr2 = Identitet(TestData.fnr3_2, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val identiteter1 = listOf(aktorId, npId, dnr, arbId)
            val identiteter2 = listOf(
                aktorId, npId, dnr.copy(gjeldende = false), fnr1.copy(gjeldende = false), fnr2, arbId
            )
            val identiteter3 = listOf(
                aktorId, npId, dnr.copy(gjeldende = false), fnr2, arbId
            )
            val aktor1 = TestData.aktor3_1
            val aktor2 = TestData.aktor3_2
            val aktor3 = TestData.aktor3_3

            // GIVEN
            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 5, aktorId.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
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
            val hendelseRows1 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows1 shouldHaveSize 1
            val hendelser1 = hendelseRows1
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser1[0].identiteter shouldBe identiteter1
            hendelser1[0].tidligereIdentiteter shouldBe emptyList()
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 5

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 6, aktorId.identitet, aktor2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
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
            val hendelseRows2 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows2 shouldHaveSize 2
            val hendelser2 = hendelseRows2
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser2[0].identiteter shouldBe identiteter1
            hendelser2[0].tidligereIdentiteter shouldBe emptyList()
            hendelser2[1].identiteter shouldBe identiteter2
            hendelser2[1].tidligereIdentiteter shouldBe identiteter1
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 6

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 7, aktorId.identitet, aktor3)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
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
            val hendelseRows3 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows3 shouldHaveSize 3
            val hendelser3 = hendelseRows3
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser3[0].identiteter shouldBe identiteter1
            hendelser3[0].tidligereIdentiteter shouldBe emptyList()
            hendelser3[1].identiteter shouldBe identiteter2
            hendelser3[1].tidligereIdentiteter shouldBe identiteter1
            hendelser3[2].identiteter shouldBe identiteter3
            hendelser3[2].tidligereIdentiteter shouldBe identiteter2
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 7

            // GIVEN
            val records4: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 8, aktorId.identitet, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records4)

            // THEN
            val identitetRows4 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows4 shouldHaveSize 5
            identitetRows4.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = aktorId,
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = npId,
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
                    identitet = fnr2,
                    status = IdentitetStatus.SLETTET
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val hendelseRows4 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows4 shouldHaveSize 4
            val hendelser4 = hendelseRows4
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser4[0].identiteter shouldBe identiteter1
            hendelser4[0].tidligereIdentiteter shouldBe emptyList()
            hendelser4[1].identiteter shouldBe identiteter2
            hendelser4[1].tidligereIdentiteter shouldBe identiteter1
            hendelser4[2].identiteter shouldBe identiteter3
            hendelser4[2].tidligereIdentiteter shouldBe identiteter2
            hendelser4[3].identiteter shouldBe emptyList()
            hendelser4[3].tidligereIdentiteter shouldBe identiteter3
            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(fnr2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow4 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow4.offset shouldBe 8
        }

        "Skal lagre merge-konflikt for melding med dnr så for fnr for arbeidssøker med to arbeidssøkerIder" {
            /**
             * melding 1: aktorId -> dnr(gjeldende)
             * melding 2: aktorId -> dnr, fnr(gjeldende)
             * melding 3: aktorId -> null(tombstone)
             */
            // GIVEN
            val aktorId = Identitet(TestData.aktorId4, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId4, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr4, IdentitetType.FOLKEREGISTERIDENT, true)
            val fnr = Identitet(TestData.fnr4_1, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId1.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val identiteter1 = listOf(aktorId, npId, dnr, arbId1)
            val aktor1 = TestData.aktor4_1
            val aktor2 = TestData.aktor4_2

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 9, aktorId.identitet, aktor1)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
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
            val konfliktRows1 = konfliktRepository.findByAktorId(aktorId.identitet)
            konfliktRows1 shouldHaveSize 0
            val hendelseRows1 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows1 shouldHaveSize 1
            hendelseRows1.map { it.asWrapper() } shouldContainOnly listOf(
                HendelseWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    status = HendelseStatus.VENTER,
                    hendelse = IdentitetHendelseWrapper(
                        type = IDENTITETER_ENDRET_HENDELSE_TYPE,
                        identiteter = identiteter1,
                        tidligereIdentiteter = emptyList()
                    )
                )
            )

            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kfnr1 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kfnr1 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr.identitet)
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 9

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 10, aktorId.identitet, aktor2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
            val identitetRows2 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows2 shouldHaveSize 4
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
                    identitet = dnr.copy(gjeldende = false),
                    status = IdentitetStatus.MERGE
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr,
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
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr)
                )
            )
            val hendelseRows2 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows2 shouldHaveSize 1
            hendelseRows2.map { it.asWrapper() } shouldContainOnly listOf(
                HendelseWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    status = HendelseStatus.VENTER,
                    hendelse = IdentitetHendelseWrapper(
                        type = IDENTITETER_ENDRET_HENDELSE_TYPE,
                        identiteter = identiteter1,
                        tidligereIdentiteter = emptyList()
                    )
                )
            )

            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr.identitet)
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 10

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 11, aktorId.identitet, null)
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
            val identitetRows3 = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows3 shouldHaveSize 4
            identitetRows3.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId,
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId,
                    status = IdentitetStatus.SLETTET
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
                    status = IdentitetStatus.SLETTET
                )
            )
            val konfliktRows3 = konfliktRepository.findByAktorId(aktorId.identitet)
            konfliktRows3 shouldHaveSize 2
            konfliktRows3.map { it.asWrapper() } shouldContainOnly listOf(
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr)
                ),
                KonfliktWrapper(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.SLETT,
                    status = KonfliktStatus.VENTER,
                    identiteter = emptyList()
                )
            )
            val hendelseRows3 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows3 shouldHaveSize 1
            hendelseRows3.map { it.asWrapper() } shouldContainOnly listOf(
                HendelseWrapper(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    status = HendelseStatus.VENTER,
                    hendelse = IdentitetHendelseWrapper(
                        type = IDENTITETER_ENDRET_HENDELSE_TYPE,
                        identiteter = identiteter1,
                        tidligereIdentiteter = emptyList()
                    )
                )
            )

            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId1, dnr.identitet)
            val kafkaKeyRow5 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow5 shouldBe KafkaKeyRow(arbeidssoekerId2, fnr.identitet)
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 11
        }

        "Skal lagre splitt-konflikt for melding med fnr på ny aktørId for arbeidssøker" {
            /**
             * melding 1: aktorId1 -> dnr, fnr(gjeldende)
             * melding 2: aktorId2 -> fnr(gjeldende)
             */
            // GIVEN
            val aktorId1 = Identitet(TestData.aktorId7_1, IdentitetType.AKTORID, true)
            val aktorId2 = Identitet(TestData.aktorId7_2, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId7, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr7, IdentitetType.FOLKEREGISTERIDENT, false)
            val fnr = Identitet(TestData.fnr7, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            val identiteter1 = listOf(aktorId1, npId, dnr, fnr, arbId1)
            val aktor1 = TestData.aktor7_1
            val aktor2 = TestData.aktor7_2

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 12, aktorId1.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
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
            val hendelseRows1 = hendelseRepository.findByAktorId(aktorId1.identitet)
            hendelseRows1 shouldHaveSize 1
            val hendelser1 = hendelseRows1
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser1[0].identiteter shouldBe identiteter1
            hendelser1[0].tidligereIdentiteter shouldBe emptyList()
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 12

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 13, aktorId2.identitet, aktor2),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
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
            val hendelseRows2 = hendelseRepository.findByAktorId(aktorId1.identitet)
            hendelseRows2 shouldHaveSize 1
            val hendelser2 = hendelseRows2
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser2[0].identiteter shouldBe identiteter1
            hendelser2[0].tidligereIdentiteter shouldBe emptyList()
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 13
        }

        "Skal lagre endring på ideniteter med ny aktørId for arbeidssøker" {
            /**
             * melding 1: aktorId1 -> dnr(gjeldende)
             * melding 2: aktorId2 -> fnr(gjeldende)
             * melding 3: aktorId2 -> dnr, fnr(gjeldende)
             */
            val aktorId1 = Identitet(TestData.aktorId8_1, IdentitetType.AKTORID, true)
            val aktorId2 = Identitet(TestData.aktorId8_2, IdentitetType.AKTORID, true)
            val npId1 = Identitet(TestData.npId8_1, IdentitetType.NPID, true)
            val npId2 = Identitet(TestData.npId8_2, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr8, IdentitetType.FOLKEREGISTERIDENT, true)
            val fnr = Identitet(TestData.fnr8, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId1 = Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)
            kafkaKeysRepository.lagre(fnr.asIdentitetsnummer(), ArbeidssoekerId(arbeidssoekerId))
            val identiteter1 = listOf(aktorId1, npId1, dnr, arbId1)
            val identiteter2 = listOf(aktorId2, npId2, fnr, arbId1)
            val identiteter3 = listOf(
                aktorId1.copy(gjeldende = false),
                aktorId2,
                npId1.copy(gjeldende = false),
                npId2,
                dnr.copy(gjeldende = false),
                fnr,
                arbId1
            )
            val aktor1 = TestData.aktor8_1
            val aktor2 = TestData.aktor8_2
            val aktor3 = TestData.aktor8_3

            // GIVEN
            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 14, aktorId1.identitet, aktor1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
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
            val hendelseRows1 = hendelseRepository.findByAktorId(aktorId1.identitet)
            hendelseRows1 shouldHaveSize 1
            val hendelser1 = hendelseRows1
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser1[0].identiteter shouldBe identiteter1
            hendelser1[0].tidligereIdentiteter shouldBe emptyList()
            hendelseRepository.findByAktorId(aktorId2.identitet) shouldHaveSize 0
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 14

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 15, aktorId2.identitet, aktor2),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
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
            val hendelseRows2 = hendelseRepository.findByAktorId(aktorId1.identitet)
            hendelseRows2 shouldHaveSize 1
            val hendelser2 = hendelseRows2
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser2[0].identiteter shouldBe identiteter1
            hendelser2[0].tidligereIdentiteter shouldBe emptyList()
            val hendelseRows3 = hendelseRepository.findByAktorId(aktorId2.identitet)
            hendelseRows3 shouldHaveSize 1
            val hendelser3 = hendelseRows3
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser3[0].identiteter shouldBe identiteter2
            hendelser3[0].tidligereIdentiteter shouldBe emptyList()
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow3 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow3 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow4 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow4 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 15

            // GIVEN
            val records3: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 16, aktorId2.identitet, aktor3),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records3)

            // THEN
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
            val hendelseRows4 = hendelseRepository.findByAktorId(aktorId1.identitet)
            hendelseRows4 shouldHaveSize 1
            val hendelser4 = hendelseRows4
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser4[0].identiteter shouldBe identiteter1
            hendelser4[0].tidligereIdentiteter shouldBe emptyList()
            val hendelseRows5 = hendelseRepository.findByAktorId(aktorId2.identitet)
            hendelseRows5 shouldHaveSize 2
            val hendelser5 = hendelseRows5
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser5[0].identiteter shouldBe identiteter2
            hendelser5[0].tidligereIdentiteter shouldBe emptyList()
            hendelser5[1].identiteter shouldBe identiteter3
            hendelser5[1].tidligereIdentiteter shouldBe identiteter2
            kafkaKeysIdentitetRepository.find(aktorId1.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(aktorId2.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId1.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow5 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow5 shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            val kafkaKeyRow6 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow6 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow3 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow3.offset shouldBe 16
        }

        "Skal slette identiteter for tombstone-melding" {
            // GIVEN
            val aktorId = Identitet(TestData.aktorId9, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId9, IdentitetType.NPID, true)
            val dnr = Identitet(TestData.dnr9, IdentitetType.FOLKEREGISTERIDENT, false)
            val fnr = Identitet(TestData.fnr9, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
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
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 17, aktorId.identitet, null),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
            identitetRows shouldHaveSize 4
            identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = aktorId,
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = npId,
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = dnr,
                    status = IdentitetStatus.SLETTET
                ),
                IdentitetWrapper(
                    arbeidssoekerId = arbeidssoekerId,
                    aktorId = aktorId.identitet,
                    identitet = fnr,
                    status = IdentitetStatus.SLETTET
                )
            )
            konfliktRepository.findByAktorId(aktorId.identitet) shouldHaveSize 0
            val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows shouldHaveSize 1
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
            kafkaKeyRow shouldBe KafkaKeyRow(arbeidssoekerId, dnr.identitet)
            kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer()) shouldBe null
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 17
        }

        "Skal ignorere duplikate meldinger" {
            // GIVEN
            val aktorId = Identitet(TestData.aktorId10, IdentitetType.AKTORID, true)
            val npId = Identitet(TestData.npId10, IdentitetType.NPID, true)
            val fnr = Identitet(TestData.fnr10, IdentitetType.FOLKEREGISTERIDENT, true)
            val arbeidssoekerId = kafkaKeysRepository.opprett(fnr.asIdentitetsnummer())
                .fold(onLeft = { null }, onRight = { it })!!.value
            val identiteter = TestData.aktor10.identifikatorer.map { it.asIdentitet() } +
                    Identitet(arbeidssoekerId.toString(), IdentitetType.ARBEIDSSOEKERID, true)

            val records1: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 18, aktorId.identitet, TestData.aktor10),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records1)

            // THEN
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
            val hendelseRows1 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows1 shouldHaveSize 1
            val hendelser1 = hendelseRows1
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser1[0].identiteter shouldBe identiteter
            hendelser1[0].tidligereIdentiteter shouldBe emptyList()
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow1 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow1 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow1.offset shouldBe 18

            // GIVEN
            val records2: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 19, aktorId.identitet, TestData.aktor10),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records2)

            // THEN
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
            val hendelseRows2 = hendelseRepository.findByAktorId(aktorId.identitet)
            hendelseRows2 shouldHaveSize 1
            val hendelser2 = hendelseRows1
                .map { hendelseDeserializer.deserializeFromString(it.data) }
                .map { it as IdentiteterEndretHendelse }
            hendelser2[0].identiteter shouldBe identiteter
            hendelser2[0].tidligereIdentiteter shouldBe emptyList()
            kafkaKeysIdentitetRepository.find(aktorId.asIdentitetsnummer()) shouldBe null
            kafkaKeysIdentitetRepository.find(npId.asIdentitetsnummer()) shouldBe null
            val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
            kafkaKeyRow2 shouldBe KafkaKeyRow(arbeidssoekerId, fnr.identitet)
            val hwmRow2 = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow2.offset shouldBe 19
        }
    }
})