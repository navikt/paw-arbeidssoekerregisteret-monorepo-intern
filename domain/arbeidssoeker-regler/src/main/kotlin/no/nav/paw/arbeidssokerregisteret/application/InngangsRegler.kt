package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

object InngangsRegler: Regler {
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
        IkkeBosattINorgeIHenholdTilFolkeregisterloven(
            !BosattEtterFregLoven,
            vedTreff = ::muligGrunnlagForAvvisning
        )
    )

    override val standardRegel: Regel = IkkeBosattINorgeIHenholdTilFolkeregisterloven(
        vedTreff = ::muligGrunnlagForAvvisning
    )
}
