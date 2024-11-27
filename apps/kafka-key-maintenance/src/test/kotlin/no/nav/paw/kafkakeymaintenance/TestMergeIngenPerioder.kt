package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord

class TestMergeIngenPerioder : FreeSpec({
    with(ApplicationTestContext()) {
        "Test merge med ingen perioder" {
            val person1 = "12345678901"
            val person2 = "12345678902"
            val person3 = "12345678903"
            addAlias(person1, alias(person1, 0L))
            addAlias(person2, alias(person2, 1L))

            process(
                aktor(//person1 og person2 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person3, gjeldende = false),
                    id(identifikasjonsnummer = person1, gjeldende = true),
                    id(identifikasjonsnummer = person2, gjeldende = false)
                )
            ) should {
                it.size shouldBe 2
                it.avType<IdentitetsnummerSammenslaatt>() should { (key, hendelse) ->
                    key shouldBe 0L
                    hendelse.identitetsnummer shouldBe person1
                    hendelse.flyttetTilArbeidssoekerId shouldBe 1L
                    hendelse.flyttedeIdentitetsnumre shouldBe listOf(person1)
                }
                it.avType<ArbeidssoekerIdFlettetInn>() should { (key, hendelse) ->
                    key shouldBe 1L
                    hendelse.identitetsnummer shouldBe person2
                    hendelse.kilde.arbeidssoekerId shouldBe 0L
                    hendelse.kilde.identitetsnummer shouldBe setOf(person1)
                }
            }
        }
    }
})

@Suppress("UNCHECKED_CAST")
inline fun <reified A: Hendelse> List<HendelseRecord<Hendelse>>.avType(): HendelseRecord<A> =
    find { it.hendelse is A }.shouldNotBeNull().let { it as HendelseRecord<A> }