package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

val reglerForInngangIPrioritertRekkefolge: List<Regel> = listOf(
    IkkeFunnet(
        PersonIkkeFunnet,
        vedTreff = ::problem
    ),
    Doed(
        ErDoed,
        vedTreff = ::problem
    ),
    ForhaandsgodkjentAvAnsatt(
        ErForhaandsgodkjent,
        vedTreff = ::ok
    ),
    Under18Aar(
        ErUnder18Aar,
        BosattEtterFregLoven,
        vedTreff = ::problem
    ),
    UkjentAlder(
        UkjentFoedselsaar,
        UkjentFoedselsdato,
        vedTreff = ::problem
    ),
    Savnet(
        ErSavnet,
        vedTreff = ::problem
    ),
    Over18AarOgBosattEtterFregLoven(
        ErOver18Aar,
        BosattEtterFregLoven,
        vedTreff = ::ok
    ),
    IkkeBosattINorgeIHenholdTilFolkeregisterloven(
        vedTreff = ::problem
    )
)
