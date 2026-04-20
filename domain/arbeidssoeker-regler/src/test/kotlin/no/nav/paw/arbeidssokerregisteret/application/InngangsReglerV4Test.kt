package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.felles.collection.PawNonEmptyList

private typealias AvvistV4 = Either.Left<PawNonEmptyList<Problem>>
private typealias GodkjentV4 = Either.Right<GrunnlagForGodkjenning>

class InngangsReglerV4Test : FreeSpec({
    "InngangsReglerV4" - {
        "godkjenner all EU/EØS-statsborger over 18 år, selv med status utflyttet (IkkeBosatt i pdl)" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErOver18Aar,
                    ErEuEoesStatsborger,
                    IkkeBosatt
                )
            ) should { result ->
                result.shouldBeInstanceOf<GodkjentV4>()
                result.value.regel.id shouldBe EuEoesStatsborgerOver18Aar
            }
        }

        "godkjenner all EU/EØS-statsborger som også er Norske statsborgere  over 18 år, selv med status utflyttet (IkkeBosatt i pdl)" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErOver18Aar,
                    ErEuEoesStatsborger,
                    IkkeBosatt,
                    ErNorskStatsborger
                )
            ) should { result ->
                result.shouldBeInstanceOf<GodkjentV4>()
                result.value.regel.id shouldBe EuEoesStatsborgerOver18Aar
            }
        }

        "avviser over 18 år som ikke er bosatt når EU/EØS-opplysning mangler" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErOver18Aar,
                    IkkeBosatt
                )
            ) should { result ->
                result.shouldBeInstanceOf<AvvistV4>()
                result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                    IkkeBosattINorgeIHenholdTilFolkeregisterloven
                )
            }
        }

        "godkjenner EU/EØS-statsborger over 18 år når BosattEtterFregLoven mangler" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErOver18Aar,
                    ErEuEoesStatsborger
                )
            ) should { result ->
                result.shouldBeInstanceOf<GodkjentV4>()
                result.value.regel.id shouldBe EuEoesStatsborgerOver18Aar
            }
        }

        "avviser under 18 år selv om bosatt etter freg loven" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErUnder18Aar,
                    BosattEtterFregLoven
                )
            ) should { result ->
                result.shouldBeInstanceOf<AvvistV4>()
                result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                    Under18Aar
                )
            }
        }

        "avviser under 18 år selv om personen er EU/EØS-opplysning borger" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErUnder18Aar,
                    ErEuEoesStatsborger
                )
            ) should { result ->
                result.shouldBeInstanceOf<AvvistV4>()
                result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                    Under18Aar
                )
            }
        }

        "avviser tredjelands borgere under 18 år som ikke er bosatt" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErUnder18Aar
                )
            ) should { result ->
                result.shouldBeInstanceOf<AvvistV4>()
                result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                    Under18Aar,
                    IkkeBosattINorgeIHenholdTilFolkeregisterloven
                )
            }
        }

        "avviser tredjelands borgere over 18 år som ikke er bosatt" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErOver18Aar
                )
            ) should { result ->
                result.shouldBeInstanceOf<AvvistV4>()
                result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                    IkkeBosattINorgeIHenholdTilFolkeregisterloven
                )
            }
        }

        "godtar forhåndsgodkjent tredjelands borgere under 18 år som ikke er bosatt" {
            InngangsReglerV4.evaluer(
                listOf(
                    ErUnder18Aar,
                    ErForhaandsgodkjent
                )
            ) should { result ->
                result.shouldBeInstanceOf<GodkjentV4>()
                result.value.regel.id shouldBe ForhaandsgodkjentAvAnsatt
            }
        }
    }
})
