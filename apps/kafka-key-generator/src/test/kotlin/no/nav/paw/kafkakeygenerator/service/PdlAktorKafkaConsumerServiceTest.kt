package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.justRun
import io.mockk.verify
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asRecords
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

        beforeTest {
            clearAllMocks()
            justRun {
                identitetServiceMock.identiteterSkalOppdateres(
                    aktorId = any<String>(),
                    identiteter = any<List<Identitet>>(),
                    sourceTimestamp = any<Instant>()
                )
            }
            justRun {
                identitetServiceMock.identiteterSkalSlettes(
                    aktorId = any<String>(),
                    sourceTimestamp = any<Instant>()
                )
            }
        }
        afterTest {
            confirmVerified(identitetServiceMock)
        }

        "Skal ignorere meldinger med offset som ikke er over HWM" {
            // GIVEN
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 1, TestData.aktorId1.identitet, TestData.aktor1_1),
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 2, TestData.aktorId1.identitet, TestData.aktor1_2)
            ).asRecords()

            // WHEN
            pdlAktorKafkaHwmOperations.updateHwm(aktorTopic, 0, 2, Instant.now())
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 2
            verify { identitetServiceMock wasNot Called }
        }

        "Skal oppdatere identiteter" {
            // GIVEN
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 3, TestData.aktorId2.identitet, TestData.aktor2_1),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 3
            verify {
                identitetServiceMock.identiteterSkalOppdateres(
                    aktorId = any<String>(),
                    identiteter = any<List<Identitet>>(),
                    sourceTimestamp = any<Instant>()
                )
            }
        }

        "Skal slette identiteter" {
            // GIVEN
            val records: ConsumerRecords<Any, Aktor> = listOf(
                ConsumerRecord<Any, Aktor>(aktorTopic, 0, 4, TestData.aktorId2.identitet, null),
            ).asRecords()

            // WHEN
            pdlAktorKafkaConsumerService.handleRecords(records)

            // THEN
            val hwmRow = pdlAktorKafkaHwmOperations.getHwm(aktorTopic, 0)
            hwmRow.offset shouldBe 4
            verify {
                identitetServiceMock.identiteterSkalSlettes(
                    aktorId = any<String>(),
                    sourceTimestamp = any<Instant>()
                )
            }
        }
    }
})