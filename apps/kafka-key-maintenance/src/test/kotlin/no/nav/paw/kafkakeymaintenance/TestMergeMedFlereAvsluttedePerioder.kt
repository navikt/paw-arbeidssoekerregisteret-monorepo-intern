package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt

class TestMergeMedFlereAvsluttedePerioder : FreeSpec({
    with(ApplicationTestContext()) {
        "Test merge med flere avsluttede perioder" {
            val person1 = "12345678901" to 0L
            val person2 = "12345678902" to 1L
            val person3 = "12345678903" to 2L
            val person4 = "12345678904" to 0L
            addAlias(person1)
            addAlias(person4)
            addAlias(person2)
            addAlias(person3)
            addPeriode(
                testPeriode(
                    identitetsnummer = person2.identitetsnummer(),
                    fra = "2021-12-04T12:00:00Z",
                    til = "2021-12-08T12:00:00Z"
                )
            )
            addPeriode(
                testPeriode(
                    identitetsnummer = person2.identitetsnummer(),
                    fra = "2021-11-09T12:00:00Z",
                    til = "2021-12-11T12:00:00Z"
                )
            )
            addPeriode(
                testPeriode(
                    identitetsnummer = person3.identitetsnummer(),
                    fra = "2021-11-08T12:00:00Z",
                    til = "2021-12-12T12:00:00Z"
                )
            )
            process(
                aktor(//person1 og person2 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person3.identitetsnummer(), gjeldende = false),
                    id(identifikasjonsnummer = person1.identitetsnummer(), gjeldende = false),
                    id(identifikasjonsnummer = person2.identitetsnummer(), gjeldende = true),
                    id(identifikasjonsnummer = person4.identitetsnummer(), gjeldende = false)
                )
            ) should { hendelser ->
                hendelser.size shouldBe 4
                hendelser.find { (key, hendelse) -> key == person1.arbeidssoekerId() && hendelse is IdentitetsnummerSammenslaatt }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person1.identitetsnummer()
                    hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                    hendelse.flyttetTilArbeidssoekerId shouldBe person3.arbeidssoekerId()
                    hendelse.alleIdentitetsnummer shouldContainExactlyInAnyOrder listOf(person1.identitetsnummer(), person4.identitetsnummer())
                }
                hendelser.find { (_, hendelse) -> hendelse is ArbeidssoekerIdFlettetInn && hendelse.kilde.arbeidssoekerId == person1.arbeidssoekerId() }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person3.identitetsnummer()
                    hendelse.shouldBeInstanceOf<ArbeidssoekerIdFlettetInn>()
                    hendelse.kilde.arbeidssoekerId shouldBe person1.arbeidssoekerId()
                    hendelse.kilde.identitetsnummer shouldContainExactlyInAnyOrder listOf(
                        person1.identitetsnummer(),
                        person4.identitetsnummer()
                    )
                }
                hendelser.find { (key, hendelse) -> key == person2.arbeidssoekerId() && hendelse is IdentitetsnummerSammenslaatt }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person2.identitetsnummer()
                    hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                    hendelse.flyttetTilArbeidssoekerId shouldBe person3.arbeidssoekerId()
                    hendelse.alleIdentitetsnummer shouldBe listOf(person2.identitetsnummer())
                }
                hendelser.find { (_, hendelse) -> hendelse is ArbeidssoekerIdFlettetInn && hendelse.kilde.arbeidssoekerId == person2.arbeidssoekerId() }
                    .shouldNotBeNull() should { (_, hendelse) ->
                    hendelse.identitetsnummer shouldBe person3.identitetsnummer()
                    hendelse.shouldBeInstanceOf<ArbeidssoekerIdFlettetInn>()
                    hendelse.kilde.identitetsnummer shouldBe setOf(person2.identitetsnummer())
                }
            }
        }
    }
})
