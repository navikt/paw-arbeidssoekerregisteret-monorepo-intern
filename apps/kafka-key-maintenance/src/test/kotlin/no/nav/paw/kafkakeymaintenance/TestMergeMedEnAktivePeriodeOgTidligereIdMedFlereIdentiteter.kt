package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.person.pdl.aktor.v2.Type

class TestMergeMedEnAktivePeriodeOgTidligereIdMedFlereIdentiteter : FreeSpec({
    with(ApplicationTestContext()) {
        "Test merge med aktive periode på den ene arbeidssøker iden og flere identiteter på den andre" {
            val person1 = "12345678901" to 0L
            val person2 = "12345678902" to 1L
            val person3 = "12345678903" to 1L
            val aktorId = "17419874198712"
            addAlias(person1)
            addAlias(person2)
            addAlias(person3)
            addAlias(aktorId to 1L)
            addPeriode(testPeriode(
                identitetsnummer = person1.identitetsnummer(),
                fra = "2021-12-04T12:00:00Z"
            ))
            addPeriode(testPeriode(
                identitetsnummer = person2.identitetsnummer(),
                fra = "2021-12-05T12:00:00Z",
                til = "2021-12-08T12:00:00Z"
            ))

            process(
                aktor(//person 1, 2 og 3 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person3.identitetsnummer(), gjeldende = true),
                    id(identifikasjonsnummer = person1.identitetsnummer(), gjeldende = false),
                    id(identifikasjonsnummer = person2.identitetsnummer(), gjeldende = false),
                    id(identifikasjonsnummer = aktorId, type = Type.AKTORID, gjeldende = true)
                )
            ) should {
                it.size shouldBe 2
                it.avType<IdentitetsnummerSammenslaatt>() should { (key, hendelse) ->
                    key shouldBe 1L
                    hendelse.identitetsnummer shouldBeIn  setOf(person2.identitetsnummer(), person3.identitetsnummer())
                    hendelse.flyttetTilArbeidssoekerId shouldBe 0L
                    hendelse.alleIdentitetsnummer shouldContainExactlyInAnyOrder listOf(
                        person2.identitetsnummer(), person3.identitetsnummer(), aktorId
                    )
                }
                it.avType<ArbeidssoekerIdFlettetInn>() should { (key, hendelse) ->
                    key shouldBe 0L
                    hendelse.identitetsnummer shouldBe person1.identitetsnummer()
                    hendelse.kilde.arbeidssoekerId shouldBe 1L
                    hendelse.kilde.identitetsnummer shouldContainExactlyInAnyOrder  setOf(
                        person2.identitetsnummer(), person3.identitetsnummer(), aktorId
                    )
                }
            }
        }
    }
})
