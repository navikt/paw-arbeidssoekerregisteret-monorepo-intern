package no.nav.paw.kafkakeygenerator.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.plugin.custom.flywayMigrate
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.asConsumerRecords
import no.nav.paw.kafkakeygenerator.test.initTestDatabase
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

class KafkaConsumerServiceTest : FreeSpec({

    lateinit var dataSource: DataSource
    lateinit var kafkaKeysRepository: KafkaKeysRepository
    lateinit var kafkaKeysAuditRepository: KafkaKeysAuditRepository
    lateinit var kafkaConsumerService: KafkaConsumerService

    beforeSpec {
        dataSource = initTestDatabase()
        dataSource.flywayMigrate()
        val database = Database.connect(dataSource)
        val healthIndicatorRepository = HealthIndicatorRepository()
        kafkaKeysRepository = KafkaKeysRepository(database)
        kafkaKeysAuditRepository = KafkaKeysAuditRepository(database)
        kafkaConsumerService = KafkaConsumerService(
            database = database,
            meterRegistry = LoggingMeterRegistry(),
            healthIndicatorRepository = healthIndicatorRepository,
            identitetRepository = IdentitetRepository(database),
            kafkaKeysRepository = kafkaKeysRepository,
            kafkaKeysAuditRepository = kafkaKeysAuditRepository
        )
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Skal ignorere hendelse av irrelevant type" {
        val identitetsnummer1 = Identitetsnummer("01017012345")
        val identitetsnummer2 = Identitetsnummer("02017012345")
        val arbeidssoekerId1 = ArbeidssoekerId(1)
        val arbeidssoekerId2 = ArbeidssoekerId(2)
        val hendelser: List<Hendelse> = listOf(
            TestData.getPeriodeStartet(identitetsnummer1, arbeidssoekerId1),
            TestData.getPeriodeAvsluttet(identitetsnummer1, arbeidssoekerId1),
            TestData.getPeriodeStartAvvist(identitetsnummer1, arbeidssoekerId1),
            TestData.getPeriodeAvsluttetAvvist(identitetsnummer1, arbeidssoekerId1),
            TestData.getArbeidssoekerIdFlettetInn(
                listOf(identitetsnummer1, identitetsnummer2),
                arbeidssoekerId1,
                arbeidssoekerId2
            )
        )

        kafkaConsumerService.handleRecords(hendelser.asConsumerRecords())

        val keyResult = kafkaKeysRepository.hent(identitetsnummer1)
        val auditResult = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer1)

        keyResult.onLeft { it shouldBe Failure("database", FailureCode.DB_NOT_FOUND) }
        keyResult.onRight { it shouldBe null }
        auditResult shouldHaveSize 0
    }

    "Skal ignorere hendelse for ukjent identitetsnummer" {
        val identitetsnummer = Identitetsnummer("03017012345")
        val fraArbeidssoekerId = ArbeidssoekerId(3)
        val tilArbeidssoekerId = ArbeidssoekerId(4)

        val hendelser: List<Hendelse> = listOf(
            TestData.getIdentitetsnummerSammenslaatt(listOf(identitetsnummer), fraArbeidssoekerId, tilArbeidssoekerId)
        )

        shouldThrow<IllegalStateException> {
            kafkaConsumerService.handleRecords(hendelser.asConsumerRecords())
        }

        val keyResult = kafkaKeysRepository.hent(identitetsnummer)
        val auditResult = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer)

        keyResult.onLeft { it shouldBe Failure("database", FailureCode.DB_NOT_FOUND) }
        keyResult.onRight { it shouldBe null }
        auditResult shouldHaveSize 0
    }

    "Skal håndtere at det er konflikt mellom arbeidssøkerId i hendelse og database" {
        val identitetsnummer1 = Identitetsnummer("04017012345")
        val identitetsnummer2 = Identitetsnummer("05017012345")
        val identitetsnummer3 = Identitetsnummer("06017012345")

        val opprettResult1 = kafkaKeysRepository.opprett(identitetsnummer1)
        opprettResult1.onLeft { it shouldBe null }
        opprettResult1.onRight { fraArbeidssoekerId ->
            val opprettResult2 = kafkaKeysRepository.opprett(identitetsnummer2)
            opprettResult2.onLeft { it shouldBe null }
            opprettResult2.onRight { tilArbeidssoekerId ->
                val opprettResult3 = kafkaKeysRepository.opprett(identitetsnummer3)
                opprettResult3.onLeft { it shouldBe null }
                opprettResult3.onRight { eksisterendeArbeidssoekerId ->
                    val hendelser: List<Hendelse> = listOf(
                        TestData.getIdentitetsnummerSammenslaatt(
                            listOf(identitetsnummer2, identitetsnummer3),
                            fraArbeidssoekerId,
                            tilArbeidssoekerId
                        )
                    )

                    kafkaConsumerService.handleRecords(hendelser.asConsumerRecords())

                    val keyResult1 = kafkaKeysRepository.hent(identitetsnummer1)
                    val keyResult2 = kafkaKeysRepository.hent(identitetsnummer2)
                    val keyResult3 = kafkaKeysRepository.hent(identitetsnummer3)
                    val auditResult1 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer1)
                    val auditResult2 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer2)
                    val auditResult3 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer3)

                    keyResult1.onLeft { it shouldBe null }
                    keyResult2.onLeft { it shouldBe null }
                    keyResult3.onLeft { it shouldBe null }
                    keyResult1.onRight { it shouldBe fraArbeidssoekerId }
                    keyResult2.onRight { it shouldBe tilArbeidssoekerId }
                    keyResult3.onRight { it shouldBe eksisterendeArbeidssoekerId }
                    auditResult1 shouldHaveSize 0
                    auditResult2 shouldHaveSize 1
                    auditResult3 shouldHaveSize 1
                    val audit2 = auditResult2.first()
                    val audit3 = auditResult3.first()
                    audit2.identitetsnummer shouldBe identitetsnummer2
                    audit2.identitetStatus shouldBe IdentitetStatus.VERIFISERT
                    audit2.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                    audit3.identitetsnummer shouldBe identitetsnummer3
                    audit3.identitetStatus shouldBe IdentitetStatus.KONFLIKT
                    audit3.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                }
            }
        }
    }

    "Skal oppdatere arbeidssøkerId for identitetsnummer" {
        val identitetsnummer1 = Identitetsnummer("07017012345")
        val identitetsnummer2 = Identitetsnummer("08017012345")
        val identitetsnummer3 = Identitetsnummer("09017012345")

        val opprettResult1 = kafkaKeysRepository.opprett(identitetsnummer1)
        opprettResult1.onLeft { it shouldBe null }
        opprettResult1.onRight { tilArbeidssoekerId ->
            val opprettResult2 = kafkaKeysRepository.opprett(identitetsnummer2)
            opprettResult2.onLeft { it shouldBe null }
            opprettResult2.onRight { fraArbeidssoekerId ->
                val hendelser: List<Hendelse> = listOf(
                    TestData.getIdentitetsnummerSammenslaatt(
                        listOf(identitetsnummer1, identitetsnummer2, identitetsnummer3),
                        fraArbeidssoekerId,
                        tilArbeidssoekerId
                    )
                )

                kafkaConsumerService.handleRecords(hendelser.asConsumerRecords())

                val keyResult1 = kafkaKeysRepository.hent(identitetsnummer1)
                val keyResult2 = kafkaKeysRepository.hent(identitetsnummer2)
                val keyResult3 = kafkaKeysRepository.hent(identitetsnummer3)
                val auditResult1 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer1)
                val auditResult2 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer2)
                val auditResult3 = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer3)

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
                audit1.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                audit2.identitetsnummer shouldBe identitetsnummer2
                audit2.identitetStatus shouldBe IdentitetStatus.OPPDATERT
                audit2.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                audit3.identitetsnummer shouldBe identitetsnummer3
                audit3.identitetStatus shouldBe IdentitetStatus.OPPRETTET
                audit3.tidligereArbeidssoekerId shouldBe tilArbeidssoekerId
            }
        }
    }
})