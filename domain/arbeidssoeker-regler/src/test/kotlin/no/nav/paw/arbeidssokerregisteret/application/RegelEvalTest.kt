package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.NonEmptyList
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning

typealias Avvist = Either.Left<NonEmptyList<Problem>>
typealias Godkjent = Either.Right<GrunnlagForGodkjenning>

class RegelEvalTest : FreeSpec({
    "Verifiser regel evaluering" - {
        "Person under 18 år" - {
            "og forhåndsgodkjent" - {
                "skal avvises når" - {
                    "er doed" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.ErDoed,
                                DomeneOpplysning.ErForhaandsgodkjent,
                                DomeneOpplysning.ErUnder18Aar
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                                Doed,
                                Under18Aar,
                                IkkeBosattINorgeIHenholdTilFolkeregisterloven
                            )
                        }
                    }
                    "er savnet" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.ErSavnet,
                                DomeneOpplysning.ErForhaandsgodkjent,
                                DomeneOpplysning.ErUnder18Aar
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                                Savnet,
                                Under18Aar,
                                IkkeBosattINorgeIHenholdTilFolkeregisterloven
                            )
                        }
                    }
                    "ikke funnet i PDL" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.PersonIkkeFunnet
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                                IkkeFunnet
                            )
                        }
                    }
                }
                "skal godkjennes når" - {
                    "ikke bosatt" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.IkkeBosatt,
                                DomeneOpplysning.ErUnder18Aar,
                                DomeneOpplysning.ErForhaandsgodkjent
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Godkjent>()
                        }
                    }
                    "ingen informasjon om bosatt" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.ErUnder18Aar,
                                DomeneOpplysning.ErForhaandsgodkjent
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Godkjent>()
                        }
                    }
                }
            }
        }
        "Person over 18 år" - {
            "skal avvises når" - {
                "Norsk statsborger, ikke bosatt" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErNorskStatsborger,
                            DomeneOpplysning.ErEuEoesStatsborger,
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id } shouldContainExactlyInAnyOrder listOf(
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "3. lands borger, ikke bosatt" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id } shouldContainExactlyInAnyOrder listOf(
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "EU/EØS borger (ikke Norsk) som har 'ikke bosatt'" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.ErEuEoesStatsborger,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id } shouldContainExactlyInAnyOrder listOf(
                            EuEoesStatsborgerMenHarStatusIkkeBosatt,
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
            }
            "skal godkjennes når" - {
                "bosatt" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.BosattEtterFregLoven
                        )
                    ).shouldBeInstanceOf<Godkjent>()
                }
                "forhåndsgodkjent" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt,
                            DomeneOpplysning.ErForhaandsgodkjent
                        )
                    ).shouldBeInstanceOf<Godkjent>()
                }
                "EU/EØS borger (ikke Norsk) som ikke har 'ikke bosatt'" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.ErEuEoesStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Godkjent>()
                    }
                }
            }
        }
    }
})