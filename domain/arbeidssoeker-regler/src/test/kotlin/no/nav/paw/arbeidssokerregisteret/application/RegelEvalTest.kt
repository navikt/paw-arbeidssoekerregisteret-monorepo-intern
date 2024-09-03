package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.NonEmptyList
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning

typealias Avvist = Either.Left<NonEmptyList<Problem>>
typealias Godkjent = Either.Right<GrunnlagForGodkjenning>

class RegelEvalTest : FreeSpec({
    "Verifiser regel evaluering" - {
        "Person under 18 år" - {
            "ikke forhåndsgodkjent av veileder" - {
                "avvises selv om alt annet er ok" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.BosattEtterFregLoven,
                            DomeneOpplysning.HarNorskAdresse,
                            DomeneOpplysning.ErNorskStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                            Under18Aar
                        )
                    }
                }
                "avvises med 'under 18 år' og 'ikke bosatt' når ikke bosatt etter f.reg. loven" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.IkkeBosatt
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                            Under18Aar,
                            IkkeBosattINorgeIHenholdTilFolkeregisterloven
                        )
                    }
                }
                "eu/eoes(men ikke Norsk statsborger) avvises med 'under 18 år' og 'ikke bosatt' når ikke bosatt etter f.reg. loven" {
                    InngangsRegler.evaluer(
                        listOf(
                            DomeneOpplysning.ErUnder18Aar,
                            DomeneOpplysning.IkkeBosatt,
                            DomeneOpplysning.ErEuEoesStatsborger
                        )
                    ) should { result ->
                        result.shouldBeInstanceOf<Avvist>()
                        result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                            Under18Aar,
                            EuEoesStatsborgerMenHarStatusIkkeBosatt
                        )
                    }
                }
            }
            "er ikke norsk EU/EØS statsborger under 18 år med dnummer og ikke utflyttet" {
                InngangsRegler.evaluer(
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
                    result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                        Under18Aar
                    )
                }
            }
            "er ikke norsl EU/EØS statsborger under 18 år med dnummer og utflyttet" {
                InngangsRegler.evaluer(
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
                    result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                        Under18Aar,
                        EuEoesStatsborgerMenHarStatusIkkeBosatt
                    )
                }
            }
            "og er forhåndsgodkjent av veileder" - {
                "skal avvises når" - {
                    "er doed" {
                        InngangsRegler.evaluer(
                            listOf(
                                DomeneOpplysning.ErDoed,
                                DomeneOpplysning.ErForhaandsgodkjent,
                                DomeneOpplysning.ErUnder18Aar,
                                DomeneOpplysning.BosattEtterFregLoven
                            )
                        ) should { result ->
                            result.shouldBeInstanceOf<Avvist>()
                            result.value.map { problem -> problem.regel.id } shouldContainExactlyInAnyOrder listOf(
                                Doed,
                                Under18Aar
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
                            EuEoesStatsborgerMenHarStatusIkkeBosatt
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