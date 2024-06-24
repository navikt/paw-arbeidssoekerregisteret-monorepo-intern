package no.nav.paw.arbeidssokerregisteret.application

interface RegelId {
    val id: String
}

sealed interface DomeneRegelId : RegelId

data object IkkeFunnet : DomeneRegelId {
    override val id: String = "IKKE_FUNNET"
}

data object Savnet : DomeneRegelId {
    override val id: String = "SAVNET"
}

data object Doed : DomeneRegelId {
    override val id: String = "DOED"
}

data object Under18Aar : DomeneRegelId {
    override val id: String = "UNDER_18_AAR"
}

data object IkkeBosattINorgeIHenholdTilFolkeregisterloven : DomeneRegelId {
    override val id: String = "IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN"
}

data object ForhaandsgodkjentAvAnsatt : DomeneRegelId {
    override val id: String = "FORHAANDSGODKJENT_AV_ANSATT"
}

data object Over18AarOgBosattEtterFregLoven : DomeneRegelId {
    override val id: String = "OVER_18_AAR_OG_BOSATT_ETTER_FREG_LOVEN"
}

data object UkjentAlder : DomeneRegelId {
    override val id: String = "UKJENT_ALDER"
}