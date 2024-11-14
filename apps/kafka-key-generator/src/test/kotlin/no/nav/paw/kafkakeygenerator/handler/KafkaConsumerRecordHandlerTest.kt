package no.nav.paw.kafkakeygenerator.handler

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafkakeygenerator.Failure
import no.nav.paw.kafkakeygenerator.FailureCode
import no.nav.paw.kafkakeygenerator.KafkaKeys
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asConsumerRecordsSequence
import no.nav.paw.kafkakeygenerator.test.initTestDatabase
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

class KafkaConsumerRecordHandlerTest : FreeSpec({

    lateinit var dataSource: DataSource
    lateinit var kafkaKeys: KafkaKeys
    lateinit var kafkaKeysAuditRepository: KafkaKeysAuditRepository
    lateinit var kafkaConsumerRecordHandler: KafkaConsumerRecordHandler

    beforeSpec {
        dataSource = initTestDatabase()
        val database = Database.connect(dataSource)
        kafkaKeys = KafkaKeys(database)
        kafkaKeysAuditRepository = KafkaKeysAuditRepository(database)
        kafkaConsumerRecordHandler = KafkaConsumerRecordHandler(
            database = database,
            kafkaKeysRepository = KafkaKeysRepository(database),
            kafkaKeysAuditRepository = kafkaKeysAuditRepository
        )
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Skal ignorere hendelse av irrelevant type" {
        val identitetsnummer = Identitetsnummer("01017012345")
        val arbeidssoekerId = ArbeidssoekerId(1)
        val hendelser: List<Hendelse> = listOf(
            TestData.getPeriodeStartet(identitetsnummer, arbeidssoekerId),
            TestData.getPeriodeAvsluttet(identitetsnummer, arbeidssoekerId),
            TestData.getPeriodeStartAvvist(identitetsnummer, arbeidssoekerId),
            TestData.getPeriodeAvsluttetAvvist(identitetsnummer, arbeidssoekerId),
            TestData.getIdentitetsnummerOpphoert(identitetsnummer, arbeidssoekerId)
        )

        kafkaConsumerRecordHandler.handleRecords(hendelser.asConsumerRecordsSequence())

        val keyResult = kafkaKeys.hent(identitetsnummer)
        val auditResult = kafkaKeysAuditRepository.find(identitetsnummer)

        keyResult.onLeft { it shouldBe Failure("database", FailureCode.DB_NOT_FOUND) }
        keyResult.onRight { it shouldBe null }
        auditResult shouldHaveSize 0
    }

    "Skal ignorere hendelse for ukjent identitetsnummer" {
        val identitetsnummer = Identitetsnummer("02017012345")
        val fraArbeidssoekerId = ArbeidssoekerId(2)
        val tilArbeidssoekerId = ArbeidssoekerId(3)

        val hendelser: List<Hendelse> = listOf(
            TestData.getIdentitetsnummerSammenslaatt(listOf(identitetsnummer), fraArbeidssoekerId, tilArbeidssoekerId)
        )

        shouldThrow<IllegalStateException> {
            kafkaConsumerRecordHandler.handleRecords(hendelser.asConsumerRecordsSequence())
        }

        val keyResult = kafkaKeys.hent(identitetsnummer)
        val auditResult = kafkaKeysAuditRepository.find(identitetsnummer)

        keyResult.onLeft { it shouldBe Failure("database", FailureCode.DB_NOT_FOUND) }
        keyResult.onRight { it shouldBe null }
        auditResult shouldHaveSize 0
    }

    "Skal oppdatere arbeidssøkerId for identitetsnummer" {
        val identitetsnummer1 = Identitetsnummer("03017012345")
        val identitetsnummer2 = Identitetsnummer("04017012345")
        val identitetsnummer3 = Identitetsnummer("05017012345")

        val opprettResult1 = kafkaKeys.opprett(identitetsnummer1)
        opprettResult1.onLeft { it shouldBe null }
        opprettResult1.onRight { tilArbeidssoekerId ->
            val opprettResult2 = kafkaKeys.opprett(identitetsnummer2)
            opprettResult2.onLeft { it shouldBe null }
            opprettResult2.onRight { fraArbeidssoekerId ->
                val hendelser: List<Hendelse> = listOf(
                    TestData.getIdentitetsnummerSammenslaatt(
                        listOf(identitetsnummer1, identitetsnummer2, identitetsnummer3),
                        fraArbeidssoekerId,
                        tilArbeidssoekerId
                    )
                )

                kafkaConsumerRecordHandler.handleRecords(hendelser.asConsumerRecordsSequence())

                val keyResult1 = kafkaKeys.hent(identitetsnummer1)
                val keyResult2 = kafkaKeys.hent(identitetsnummer2)
                val keyResult3 = kafkaKeys.hent(identitetsnummer3)
                val auditResult1 = kafkaKeysAuditRepository.find(identitetsnummer1)
                val auditResult2 = kafkaKeysAuditRepository.find(identitetsnummer2)
                val auditResult3 = kafkaKeysAuditRepository.find(identitetsnummer3)

                keyResult1.onLeft { it shouldBe null }
                keyResult2.onLeft { it shouldBe null }
                keyResult3.onLeft { it shouldBe null }
                keyResult1.onRight { it shouldBe tilArbeidssoekerId }
                keyResult2.onRight { it shouldBe tilArbeidssoekerId }
                keyResult3.onRight { it shouldBe tilArbeidssoekerId }
                auditResult1 shouldHaveSize 1
                auditResult2 shouldHaveSize 1
                auditResult3 shouldHaveSize 1
                val audit1 = auditResult1.first()
                val audit2 = auditResult2.first()
                val audit3 = auditResult3.first()
                audit1.identitetsnummer shouldBe identitetsnummer1
                audit1.identitetStatus shouldBe IdentitetStatus.VERIFISERT
                audit2.identitetsnummer shouldBe identitetsnummer2
                audit2.identitetStatus shouldBe IdentitetStatus.OPPDATERT
                audit3.identitetsnummer shouldBe identitetsnummer3
                audit3.identitetStatus shouldBe IdentitetStatus.OPPRETTET
            }
        }
    }
})