package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.test.minutes
import java.time.Instant.parse

class TestMergeMedFlereAvsluttedePerioder : FreeSpec({
    val testTime = parse("2021-12-12T12:00:00Z")
    with(initTopologyTestContext(testTime)) {
        "Test merge med flere avsluttede perioder" {
            val person1 = "12345678901"
            val person2 = "12345678902"
            val person3 = "12345678903"
            val person4 = "12345678904"
            addAlias(person1, alias(person1, 0L))
            addAlias(person4, alias(person4, 0L))
            addAlias(person2, alias(person2, 1L))
            addAlias(person3, alias(person3, 2L))
            addPeriode(testPeriode(identitetsnummer = person2, fra = "2021-12-04T12:00:00Z", til = "2021-12-08T12:00:00Z"))
            addPeriode(testPeriode(identitetsnummer = person2, fra = "2021-11-09T12:00:00Z", til = "2021-12-11T12:00:00Z"))
            addPeriode(testPeriode(identitetsnummer = person3, fra = "2021-11-08T12:00:00Z", til = "2021-12-12T12:00:00Z"))
            aktorTopic.pipeInput(
                "p1",
                aktor(//person1 og person2 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person3, gjeldende = false),
                    id(identifikasjonsnummer = person1, gjeldende = false),
                    id(identifikasjonsnummer = person2, gjeldende = true),
                    id(identifikasjonsnummer = person4, gjeldende = false)
                ),
                testTime
            )
            hendelseloggTopic.isEmpty shouldBe true
            testDriver.advanceWallClockTime(aktorTopologyConfig.supressionDelay - 1.minutes)
            hendelseloggTopic.isEmpty shouldBe true
            testDriver.advanceWallClockTime(5.minutes)
            hendelseloggTopic.isEmpty shouldBe false
            hendelseloggTopic.readKeyValuesToList() should {
                it.size shouldBe 2
                it.find { (key, _) -> key == 0L }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person1
                    hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                    hendelse.flyttetTilArbeidssoekerId shouldBe 2L
                    hendelse.alleIdentitetsnummer shouldContainExactlyInAnyOrder  listOf(person1, person4)
                }
                it.find { (key, _) -> key == 1L }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person2
                    hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                    hendelse.flyttetTilArbeidssoekerId shouldBe 2L
                    hendelse.alleIdentitetsnummer shouldBe listOf(person2)
                }
            }
        }

    }
})
