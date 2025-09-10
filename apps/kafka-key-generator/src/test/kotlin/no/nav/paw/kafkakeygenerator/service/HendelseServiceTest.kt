package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.paw.identitet.internehendelser.IDENTITETER_ENDRET_V1_HENDELSE_TYPE
import no.nav.paw.identitet.internehendelser.IDENTITETER_MERGET_V1_HENDELSE_TYPE
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.util.concurrent.Future

class HendelseServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        val metadata = RecordMetadata(TopicPartition("topic", 1), 1, 1, 1, 1, 1)
        val futureMock = mockk<Future<RecordMetadata>>()
        val recordSlot = slot<ProducerRecord<Long, IdentitetHendelse>>()

        beforeSpec {
            setUp()
            every { futureMock.get() } returns metadata
            every { pawIdentitetProducerMock.send(capture(recordSlot)) } returns futureMock
        }

        afterSpec {
            tearDown()
            confirmVerified(futureMock, pawIdentitetProducerMock)
        }

        "Skal sende ventende identiteter-endret-hendelser" {
            // GIVEN
            val aktorId = TestData.aktorId1
            val npId = TestData.npId1
            val dnr = TestData.dnr1
            val fnr = TestData.fnr1_1
            val arbeidssoekerId = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId = Identitet(
                identitet = arbeidssoekerId.toString(),
                type = IdentitetType.ARBEIDSSOEKERID,
                gjeldende = true
            )
            val sendtHendelse = IdentiteterEndretHendelse(
                identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr, arbId),
                tidligereIdentiteter = listOf(aktorId, npId, dnr, arbId)
            )
            hendelseRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                version = 1,
                data = hendelseSerializer.serializeToString(sendtHendelse),
                status = HendelseStatus.VENTER
            )

            // WHEN
            hendelseService.sendVentendeIdentitetHendelser()

            // THEN
            recordSlot.isCaptured shouldBe true
            val record = recordSlot.captured
            record.key() shouldBe arbeidssoekerId
            record.value().hendelseType shouldBe IDENTITETER_ENDRET_V1_HENDELSE_TYPE
            val mottattHendelse = record.value().shouldBeTypeOf<IdentiteterEndretHendelse>()
            mottattHendelse.hendelseId shouldBe sendtHendelse.hendelseId
            mottattHendelse.hendelseType shouldBe sendtHendelse.hendelseType
            mottattHendelse.identiteter shouldBe sendtHendelse.identiteter
            mottattHendelse.tidligereIdentiteter shouldBe sendtHendelse.tidligereIdentiteter
            verify { futureMock.get() }
            verify { pawIdentitetProducerMock.send(any<ProducerRecord<Long, IdentitetHendelse>>()) }
        }

        "Skal sende ventende identiteter-merget-hendelser" {
            // GIVEN
            val aktorId = TestData.aktorId2
            val npId = TestData.npId2
            val dnr = TestData.dnr2
            val fnr = TestData.fnr2_1
            val arbeidssoekerId = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbId = Identitet(
                identitet = arbeidssoekerId.toString(),
                type = IdentitetType.ARBEIDSSOEKERID,
                gjeldende = true
            )
            val sendtHendelse = IdentiteterMergetHendelse(
                identiteter = listOf(aktorId, npId, dnr.copy(gjeldende = false), fnr, arbId),
                tidligereIdentiteter = listOf(aktorId, npId, dnr, arbId)
            )
            hendelseRepository.insert(
                arbeidssoekerId = arbeidssoekerId,
                aktorId = aktorId.identitet,
                version = 1,
                data = hendelseSerializer.serializeToString(sendtHendelse),
                status = HendelseStatus.VENTER
            )

            // WHEN
            hendelseService.sendVentendeIdentitetHendelser()

            // THEN
            recordSlot.isCaptured shouldBe true
            val record = recordSlot.captured
            record.key() shouldBe arbeidssoekerId
            record.value().hendelseType shouldBe IDENTITETER_MERGET_V1_HENDELSE_TYPE
            val mottattHendelse = record.value().shouldBeTypeOf<IdentiteterMergetHendelse>()
            mottattHendelse.hendelseId shouldBe sendtHendelse.hendelseId
            mottattHendelse.hendelseType shouldBe sendtHendelse.hendelseType
            mottattHendelse.identiteter shouldBe sendtHendelse.identiteter
            mottattHendelse.tidligereIdentiteter shouldBe sendtHendelse.tidligereIdentiteter
            verify { futureMock.get() }
            verify { pawIdentitetProducerMock.send(any<ProducerRecord<Long, IdentitetHendelse>>()) }
        }
    }
})