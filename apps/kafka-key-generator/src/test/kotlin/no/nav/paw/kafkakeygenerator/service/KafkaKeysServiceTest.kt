package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.kafkakeygenerator.api.v2.hentLokaleAlias
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.GenericFailure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Left
import no.nav.paw.kafkakeygenerator.vo.Right
import org.junit.jupiter.api.fail

class KafkaKeysServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Test suite for hentEllerOpprett()" - {
            "alle identer for person1 skal gi samme nøkkel" {
                val person1KafkaKeys = listOf(
                    TestData.dnr1,
                    TestData.fnr1_1,
                    TestData.fnr1_2,
                    TestData.aktorId1,
                ).map(::hentEllerOpprett)
                person1KafkaKeys.filterIsInstance<Left<GenericFailure>>().size shouldBe 0
                person1KafkaKeys.filterIsInstance<Right<ArbeidssoekerId>>()
                    .map { it.right }
                    .distinct().size shouldBe 1
                kafkaKeysService.hentLokaleAlias(2, listOf(TestData.dnr1))
                    .onLeft { fail { "Uventet feil: $it" } }
                    .onRight { res ->
                        res.flatMap { it.koblinger }.map { it.identitetsnummer }.shouldContainExactlyInAnyOrder(
                            TestData.dnr1, TestData.fnr1_1, TestData.fnr1_2, TestData.aktorId1
                        )
                    }
                val lokaleAlias = kafkaKeysService.hentLokaleAlias(2, Identitetsnummer(TestData.dnr1))
                //hentEllerOpprett(TestData.fnr5).shouldBeInstanceOf<Right<ArbeidssoekerId>>()
                lokaleAlias.onLeft { fail { "Uventet feil: $it" } }.onRight { alias ->
                    alias.identitetsnummer shouldBe TestData.dnr1
                    alias.koblinger.size shouldBe 4
                    alias.koblinger.any { it.identitetsnummer == TestData.dnr1 } shouldBe true
                    alias.koblinger.any { it.identitetsnummer == TestData.fnr1_1 } shouldBe true
                    alias.koblinger.any { it.identitetsnummer == TestData.fnr1_2 } shouldBe true
                    alias.koblinger.any { it.identitetsnummer == TestData.aktorId1 } shouldBe true
                }
            }
            "alle identer for person2 skal gi samme nøkkel" {
                val person2KafkaKeys = listOf(
                    TestData.dnr2,
                    TestData.fnr2_1,
                    TestData.fnr2_2,
                    TestData.aktorId2,
                ).map(::hentEllerOpprett)
                person2KafkaKeys.filterIsInstance<Left<GenericFailure>>().size shouldBe 0
                person2KafkaKeys.filterIsInstance<Right<ArbeidssoekerId>>()
                    .map { it.right }
                    .distinct().size shouldBe 1
            }
            "person1 og person2 skal ha forskjellig nøkkel" {
                hentEllerOpprett(TestData.dnr1) shouldNotBe hentEllerOpprett(TestData.aktorId2)
            }
            "ingen treff i PDL skal feile med ${FailureCode.PDL_NOT_FOUND}" {
                val person3KafkaKeys = listOf(
                    TestData.dnr6,
                    TestData.fnr5
                ).map(::hentEllerOpprett)
                person3KafkaKeys.filterIsInstance<Left<Failure>>().size shouldBe 2
                person3KafkaKeys.filterIsInstance<Right<Long>>().size shouldBe 0
                person3KafkaKeys.forEach {
                    it.shouldBeInstanceOf<Left<Failure>>()
                    it.left.code() shouldBe FailureCode.PDL_NOT_FOUND
                }
            }
        }

        "Test suite for hent()" - {

        }
    }
})
