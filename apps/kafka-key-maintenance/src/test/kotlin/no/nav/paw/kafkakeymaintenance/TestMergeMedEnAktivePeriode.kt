package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt

class TestMergeMedEnAktivePeriode : FreeSpec({
    with(ApplicationTestContext()) {
        "Test merge med aktive periode på den ene arbeidssøker iden" {
            val person1 = "12345678901"
            val person2 = "12345678902"
            val person3 = "12345678903"
            addAlias(person1, alias(person1, 0L))
            addAlias(person2, alias(person2, 1L))
            addPeriode(testPeriode(identitetsnummer = person1, fra = "2021-12-04T12:00:00Z"))
            addPeriode(testPeriode(identitetsnummer = person2, fra = "2021-12-05T12:00:00Z", til = "2021-12-08T12:00:00Z"))

            process(
                aktor(//person1 og person2 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person3, gjeldende = false),
                    id(identifikasjonsnummer = person1, gjeldende = true),
                    id(identifikasjonsnummer = person2, gjeldende = false)
                )
            ) should {
                it.size shouldBe 2
                it.avType<IdentitetsnummerSammenslaatt>() should { (key, hendelse) ->
                    key shouldBe 1L
                    hendelse.identitetsnummer shouldBe person2
                    hendelse.flyttetTilArbeidssoekerId shouldBe 0L
                    hendelse.flyttedeIdentitetsnumre shouldBe listOf(person2)
                }
                it.avType<ArbeidssoekerIdFlettetInn>() should { (key, hendelse) ->
                    key shouldBe 0L
                    hendelse.identitetsnummer shouldBe person1
                    hendelse.kilde.arbeidssoekerId shouldBe 1L
                    hendelse.kilde.identitetsnummer shouldBe setOf(person2)
                }
            }
        }
    }
})
