package no.nav.paw.arbeidssokerregisteret.application

interface RegelId {
    val beskrivelse: String
}

sealed interface DomeneRegelId : RegelId

data object IkkeFunnet : DomeneRegelId {
    override val beskrivelse: String = "Person ikke funnet"
}

data object Savnet : DomeneRegelId {
    override val beskrivelse: String = "Er registrert som savnet"
}

data object Doed : DomeneRegelId {
    override val beskrivelse: String = "Er registrert som død"
}

data object Under18Aar : DomeneRegelId {
    override val beskrivelse: String = "Er under 18 år"
}

data object IkkeBosattINorgeIHenholdTilFolkeregisterloven : DomeneRegelId {
    override val beskrivelse: String = "Avvist fordi personen ikke er bosatt i Norge i henhold til folkeregisterloven"
}

data object ForhaandsgodkjentAvAnsatt : DomeneRegelId {
    override val beskrivelse: String = "Er forhåndsgodkjent av ansatt"
}

data object Over18AarOgBosattEtterFregLoven : DomeneRegelId {
    override val beskrivelse: String = "Er over 18 år, er bosatt i Norge i henhold Folkeregisterloven"
}

data object UkjentAlder : DomeneRegelId {
    override val beskrivelse: String = "Kunne ikke fastslå alder"
}

data object EuEoesStatsborgerOver18Aar : DomeneRegelId {
    override val beskrivelse: String = "Er EU/EØS statsborger"
}

/**
 * Egentlig ikke nødvendig å ha en egen regel for dette, men pga. Arena så trenger vi denne.
 * Arena sjekker om personen er EU/EØS statsborger og er utflyttet (som gir status 'ikke bosatt').
 */
data object EuEoesStatsborgerMenHarStatusIkkeBosatt : DomeneRegelId {
    override val beskrivelse: String = "Er EU/EØS statsborger, men har status 'ikke bosatt'"
}