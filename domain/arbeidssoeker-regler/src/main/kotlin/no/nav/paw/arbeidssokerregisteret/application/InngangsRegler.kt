package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

val reglerForInngangIPrioritertRekkefolge: List<Regel> = listOf(
    "Person ikke funnet"(
        PersonIkkeFunnet,
        id = IkkeFunnet,
        vedTreff = ::problem
    ),
    "Er registrert som død"(
        ErDoed,
        id = Doed,
        vedTreff = ::problem
    ),
    "Er forhåndsgodkjent av ansatt"(
        ErForhaandsgodkjent,
        id = ForhaandsgodkjentAvAnsatt,
        vedTreff = ::ok
    ),
    "Er bosatt i Norge i henhold Folkeregisterloven, men er under 18 år"(
        ErUnder18Aar,
        BosattEtterFregLoven,
        id = Under18Aar,
        vedTreff = ::problem
    ),
    "Kunne ikke fastslå alder"(
        UkjentFoedselsaar,
        UkjentFoedselsdato,
        id = UkjentAlder,
        vedTreff = ::problem
    ),
    "Er registrert som savnet"(
        ErSavnet,
        id = Savnet,
        vedTreff = ::problem
    ),
    "Er over 18 år, er bosatt i Norge i henhold Folkeregisterloven"(
        ErOver18Aar,
        BosattEtterFregLoven,
        id = Over18AarOgBosattEtterFregLoven,
        vedTreff = ::ok
    ),
    "Avvist fordi personen ikke er bosatt i Norge i henhold til folkeregisterloven"(
        id = IkkeBosattINorgeIHenholdTilFolkeregisterloven,
        vedTreff = ::problem
    )
)
