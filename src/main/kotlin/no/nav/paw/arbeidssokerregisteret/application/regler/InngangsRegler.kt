package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*

val reglerForInngangIPrioritertRekkefolge: List<Regel<EndeligResultat>> = listOf(
    "Er forhåndsgodkjent av ansatt"(
        Opplysning.FORHAANDSGODKJENT_AV_ANSATT,
        id = RegelId.FORHAANDSGODKJENT_AV_ANSATT,
        vedTreff = ::OK
    ),
    "Person ikke funnet"(
        Opplysning.PERSON_IKKE_FUNNET,
        id = RegelId.IKKE_FUNNET,
        vedTreff = ::Avvist
    ),
    "Er under 18 år"(
        Opplysning.ER_UNDER_18_AAR,
        id = RegelId.UNDER_18_AAR,
        vedTreff = ::Avvist
    ),
    "Kunne ikke fatslå alder"(
        Opplysning.UKJENT_FOEDSELSDATO,
        Opplysning.UKJENT_FOEDSELSAAR,
        id = RegelId.UKJENT_ALDER,
        vedTreff = ::Avvist
    ),
    "Er registrert som doed"(
        Opplysning.DOED,
        id = RegelId.DOED,
        vedTreff = ::Avvist
    ),
    "Er registrert som savnet"(
        Opplysning.SAVNET,
        id = RegelId.SAVNET,
        vedTreff = ::Avvist
    ),
    "Er over 18 år, er bosatt i Norge etter Folkeregisterloven"(
        Opplysning.ER_OVER_18_AAR,
        Opplysning.BOSATT_ETTER_FREG_LOVEN,
        id = RegelId.OVER_18_AAR_OG_BOSATT_ETTER_FREG_LOVEN,
        vedTreff = ::OK
    ),
    "Avvist fordi personen ikke er bosatt i Norge i henhold til folkeregisterloven"(
        id = RegelId.IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN,
        vedTreff = ::Avvist
    )
)
