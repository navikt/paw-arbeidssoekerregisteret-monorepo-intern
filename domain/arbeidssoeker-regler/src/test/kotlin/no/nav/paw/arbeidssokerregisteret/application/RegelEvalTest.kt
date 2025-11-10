package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.felles.collection.PawNonEmptyList

typealias Avvist = Either.Left<PawNonEmptyList<Problem>>
typealias Godkjent = Either.Right<GrunnlagForGodkjenning>

class RegelEvalTest : FreeSpec({
    "Verifiser regel evaluering" - {
        "Person under 18 år" - {
            "ikke forhåndsgodkjent av veileder" - {
                "avvises selv om alt annet er ok" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.BosattEtterFregLoven,
                            DomeneOpplysning.HarNorskAdresse,
                            DomeneOpplysning.ErNorskStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            Under18Aar
                        )
                    }
                }
                "avvises med 'under 18 år' og 'ikke bosatt' når ikke bosatt etter f.reg. loven og ingen statsborgerskap info" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            Under18Aar,
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "Norsk statsborger avvises med 'under 18 år' og 'ikke bosatt' når ikke bosatt etter f.reg. loven" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.IkkeBosatt,
                            DomeneOpplysning.ErNorskStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            Under18Aar,
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "eu/eoes(men ikke Norsk statsborger) avvises med 'under 18 år' og 'ikke bosatt' når ikke bosatt etter f.reg. loven" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.IkkeBosatt,
                            DomeneOpplysning.ErEuEoesStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            Under18Aar,
                            EuEoesStatsborgerMenHarStatusIkkeBosatt
                        )
                    }
                }
            }
            "er ikke norsk EU/EØS statsborger under 18 år med dnummer og ikke utflyttet" {
                InngangsReglerV2.evaluer(
                    listOf(
                        DomeneOpplysning.ErUnder18Aar,
                        DomeneOpplysning.ErEuEoesStatsborger,
                        DomeneOpplysning.HarUtenlandskAdresse,
                        DomeneOpplysning.HarRegistrertAdresseIEuEoes,
                        DomeneOpplysning.IngenInformasjonOmOppholdstillatelse,
                        DomeneOpplysning.Dnummer,
                        DomeneOpplysning.IngenFlytteInformasjon
                    )
                ) should { result ->
                    result.shouldBeInstanceOf<Avvist>()
                    result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                        Under18Aar
                    )
                }
            }
            "er ikke norsk EU/EØS statsborger under 18 år med dnummer og utflyttet" {
                InngangsReglerV2.evaluer(
                    listOf(
                        DomeneOpplysning.ErUnder18Aar,
                        DomeneOpplysning.ErEuEoesStatsborger,
                        DomeneOpplysning.HarUtenlandskAdresse,
                        DomeneOpplysning.HarRegistrertAdresseIEuEoes,
                        DomeneOpplysning.IngenInformasjonOmOppholdstillatelse,
                        DomeneOpplysning.Dnummer,
                        DomeneOpplysning.IngenFlytteInformasjon,
                        DomeneOpplysning.IkkeBosatt
                    )
                ) should { result ->
                    result.shouldBeInstanceOf<Avvist>()
                    result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                        Under18Aar,
                        EuEoesStatsborgerMenHarStatusIkkeBosatt
                    )
                }
            }
            "og er forhåndsgodkjent av veileder" - {
                "skal avvises når" - {
                    "er doed" {
                        InngangsReglerV2.evaluer(
                            listOf(
                                DomeneOpplysning.ErDoed,
                                DomeneOpplysning.ErForhaandsgodkjent,
                                DomeneOpplysning.ErUnder18Aar,
                                DomeneOpplysning.BosattEtterFregLoven
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                                Doed,
                                Under18Aar
                            )
                        }
                    }
                    "er savnet" {
                        InngangsReglerV2.evaluer(
                            listOf(
                                DomeneOpplysning.ErSavnet,
                                DomeneOpplysning.ErForhaandsgodkjent,
                                DomeneOpplysning.ErUnder18Aar
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                                Savnet,
                                Under18Aar,
                                IkkeBosattINorgeIHenholdTilFolkeregisterloven
                            )
                        }
                    }
                    "ikke funnet i PDL" {
                        InngangsReglerV2.evaluer(
                            listOf(
                                DomeneOpplysning.PersonIkkeFunnet
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                                IkkeFunnet
                            )
                        }
                    }
                }
                "skal godkjennes når" - {
                    "ikke bosatt" {
                        InngangsReglerV2.evaluer(
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
                        InngangsReglerV2.evaluer(
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
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErNorskStatsborger,
                            DomeneOpplysning.ErEuEoesStatsborger,
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "3. lands borger, ikke bosatt" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "EU/EØS borger (ikke Norsk) som har 'ikke bosatt'" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.ErEuEoesStatsborger,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(
                            EuEoesStatsborgerMenHarStatusIkkeBosatt
                        )
                    }
                }
            }
            "skal godkjennes når" - {
                "bosatt" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.BosattEtterFregLoven
                        )
                    ).shouldBeInstanceOf<Godkjent>()
                }
                "forhåndsgodkjent" {
                    InngangsReglerV2.evaluer(
                        listOf(
                            DomeneOpplysning.ErOver18Aar,
                            DomeneOpplysning.IkkeBosatt,
                            DomeneOpplysning.ErForhaandsgodkjent
                        )
                    ).shouldBeInstanceOf<Godkjent>()
                }
                "EU/EØS borger (ikke Norsk) som ikke har 'ikke bosatt'" {
                    InngangsReglerV2.evaluer(
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
        "Statsborgere i land med og uten avtaler" - {
            "Gbr statsborger over 18 år skal godkjennes" {
                InngangsReglerV2.evaluer(
                    listOf(
                        DomeneOpplysning.ErOver18Aar,
                        DomeneOpplysning.ErGbrStatsborger
                    )
                ) should { result ->
                    result.shouldBeInstanceOf<Godkjent>()
                }
            }
            "Gbr statsborger under 18 år skal avvises" {
                InngangsReglerV2.evaluer(
                    listOf(
                        DomeneOpplysning.ErUnder18Aar,
                        DomeneOpplysning.ErGbrStatsborger
                    )
                ) should { result ->
                    result.shouldBeInstanceOf<Avvist>()
                    result.value.map { it.regel.id }.toList() shouldBe listOf(Under18Aar)
                }
            }
            "3. lands statsborger under 18 år skal avvises" {
                InngangsReglerV2.evaluer(
                    listOf(
                        DomeneOpplysning.ErUnder18Aar
                    )
                ) should { result ->
                    result.shouldBeInstanceOf<Avvist>()
                    result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(Under18Aar, IkkeBosattINorgeIHenholdTilFolkeregisterloven)
                }
            }
            "3. lands statsborger over 18 år skal avvises" {
                InngangsReglerV2.evaluer(
                    listOf()
                ) should { result ->
                    result.shouldBeInstanceOf<Avvist>()
                    result.value.map { it.regel.id }.toList() shouldContainExactlyInAnyOrder listOf(IkkeBosattINorgeIHenholdTilFolkeregisterloven)
                }
            }
        }
    }
})