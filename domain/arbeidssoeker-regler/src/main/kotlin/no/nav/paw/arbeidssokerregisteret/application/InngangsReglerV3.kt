package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

object InngangsReglerV3: Regler {
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
        Opphoert(
            OpphoertIdentitet,
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
            vedTreff = ::grunnlagForGodkjenning
        ),
        ErStatsborgerILandMedAvtale(
            ErGbrStatsborger,
            ErOver18Aar,
            vedTreff = ::grunnlagForGodkjenning
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
