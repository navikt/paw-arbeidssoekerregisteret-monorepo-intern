package no.nav.paw.kafkakeygenerator.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.AuditIdentitetStatus
import no.nav.paw.kafkakeygenerator.model.FailureCode
import no.nav.paw.kafkakeygenerator.model.GenericFailure
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asHendelseRecords

class PawHendelseKafkaConsumerServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal ignorere hendelse av irrelevant type" {
            val identitetsnummer1 = Identitetsnummer("01017012345")
            val identitetsnummer2 = Identitetsnummer("02017012345")
            val arbeidssoekerId1 = ArbeidssoekerId(1)
            val arbeidssoekerId2 = ArbeidssoekerId(2)
            val hendelser: List<Hendelse> = listOf(
                TestData.periodeStartetHendelse(identitetsnummer1, arbeidssoekerId1),
                TestData.periodeAvsluttetHendelse(identitetsnummer1, arbeidssoekerId1),
                TestData.periodeStartAvvistHendelse(identitetsnummer1, arbeidssoekerId1),
                TestData.periodeAvsluttetAvvistHendelse(identitetsnummer1, arbeidssoekerId1),
                TestData.arbeidssoekerIdFlettetInnHendelse(
                    listOf(identitetsnummer1, identitetsnummer2),
                    arbeidssoekerId1,
                    arbeidssoekerId2
                )
            )

            pawHendelseKafkaConsumerService.handleRecords(hendelser.asHendelseRecords())

            val keyResult = kafkaKeysRepository.hent(identitetsnummer1)
            val auditResult = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer1)

            keyResult.onLeft { it shouldBe GenericFailure("database", FailureCode.DB_NOT_FOUND) }
            keyResult.onRight { it shouldBe null }
            auditResult shouldHaveSize 0
        }

        "Skal ignorere hendelse for ukjent identitetsnummer" {
            val identitetsnummer = Identitetsnummer("03017012345")
            val fraArbeidssoekerId = ArbeidssoekerId(3)
            val tilArbeidssoekerId = ArbeidssoekerId(4)

            val hendelser: List<Hendelse> = listOf(
                TestData.identitetsnummerSammenslaattHendelse(
                    listOf(identitetsnummer),
                    fraArbeidssoekerId,
                    tilArbeidssoekerId
                )
            )

            shouldThrow<IllegalStateException> {
                pawHendelseKafkaConsumerService.handleRecords(hendelser.asHendelseRecords())
            }

            val keyResult = kafkaKeysRepository.hent(identitetsnummer)
            val auditResult = kafkaKeysAuditRepository.findByIdentitetsnummer(identitetsnummer)

            keyResult.onLeft { it shouldBe GenericFailure("database", FailureCode.DB_NOT_FOUND) }
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
                            TestData.identitetsnummerSammenslaattHendelse(
                                listOf(identitetsnummer2, identitetsnummer3),
                                fraArbeidssoekerId,
                                tilArbeidssoekerId
                            )
                        )

                        pawHendelseKafkaConsumerService.handleRecords(hendelser.asHendelseRecords())

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
                        audit2.identitetStatus shouldBe AuditIdentitetStatus.VERIFISERT
                        audit2.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                        audit3.identitetsnummer shouldBe identitetsnummer3
                        audit3.identitetStatus shouldBe AuditIdentitetStatus.KONFLIKT
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
                        TestData.identitetsnummerSammenslaattHendelse(
                            listOf(identitetsnummer1, identitetsnummer2, identitetsnummer3),
                            fraArbeidssoekerId,
                            tilArbeidssoekerId
                        )
                    )

                    pawHendelseKafkaConsumerService.handleRecords(hendelser.asHendelseRecords())

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
                    audit1.identitetStatus shouldBe AuditIdentitetStatus.VERIFISERT
                    audit1.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                    audit2.identitetsnummer shouldBe identitetsnummer2
                    audit2.identitetStatus shouldBe AuditIdentitetStatus.OPPDATERT
                    audit2.tidligereArbeidssoekerId shouldBe fraArbeidssoekerId
                    audit3.identitetsnummer shouldBe identitetsnummer3
                    audit3.identitetStatus shouldBe AuditIdentitetStatus.OPPRETTET
                    audit3.tidligereArbeidssoekerId shouldBe tilArbeidssoekerId
                }
            }
        }
    }
})