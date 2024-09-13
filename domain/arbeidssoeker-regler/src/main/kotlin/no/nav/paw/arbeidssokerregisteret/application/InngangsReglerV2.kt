package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

object InngangsReglerV2 : Regler {
    override val regler: List<Regel> = listOf(
        IkkeFunnet(
            PersonIkkeFunnet,
            vedTreff = ::skalAvises
        ),
        Doed(
            ErDoed,
            vedTreff = ::skalAvises
        ),
        Savnet(
            ErSavnet,
            vedTreff = ::skalAvises
        ),
        ForhaandsgodkjentAvAnsatt(
            ErForhaandsgodkjent,
            vedTreff = ::grunnlagForGodkjenning
        ),
        Under18Aar(
            ErUnder18Aar,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        UkjentAlder(
            UkjentFoedselsaar,
            UkjentFoedselsdato,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        Over18AarOgBosattEtterFregLoven(
            ErOver18Aar,
            BosattEtterFregLoven,
            vedTreff = ::grunnlagForGodkjenning
        ),
        EuEoesStatsborgerOver18Aar(
            ErOver18Aar,
            ErEuEoesStatsborger,
            !ErNorskStatsborger,
            !IkkeBosatt,
            vedTreff = ::grunnlagForGodkjenning
        ),
        ErStatsborgerILandMedAvtale(
            ErGbrStatsborger,
            ErOver18Aar,
            vedTreff = ::grunnlagForGodkjenning
        ),
        EuEoesStatsborgerMenHarStatusIkkeBosatt(
            ErEuEoesStatsborger,
            !ErNorskStatsborger,
            IkkeBosatt,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        IkkeBosattINorgeIHenholdTilFolkeregisterloven(
            !BosattEtterFregLoven,
            ErNorskEllerTredjelandsborger,
            vedTreff = ::muligGrunnlagForAvvisning
        )
    )

    override val standardRegel: Regel = IkkeBosattINorgeIHenholdTilFolkeregisterloven(
        vedTreff = ::muligGrunnlagForAvvisning
    )
}

data object ErNorskEllerTredjelandsborger : Condition {
    override fun eval(opplysninger: Iterable<Opplysning>): Boolean =
        ErNorskStatsborger in opplysninger || (ErEuEoesStatsborger !in opplysninger && ErGbrStatsborger !in opplysninger)
}

