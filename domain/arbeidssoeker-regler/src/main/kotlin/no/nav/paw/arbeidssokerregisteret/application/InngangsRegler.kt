package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

val reglerForInngangIPrioritertRekkefolge: List<Regel> = listOf(
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
        BosattEtterFregLoven,
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
    )
)

val standardInngangsregel = IkkeBosattINorgeIHenholdTilFolkeregisterloven(
    vedTreff = ::muligGrunnlagForAvvisning
)
