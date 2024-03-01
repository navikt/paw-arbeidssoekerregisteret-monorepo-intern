package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*

val reglerForInngangIPrioritertRekkefolge: List<Regel<EndeligResultat>> = listOf(
    "Er forhåndsgodkjent av ansatt"(
        Opplysning.FORHAANDSGODKJENT_AV_ANSATT,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    "Person ikke funnet"(
        Opplysning.PERSON_IKKE_FUNNET,
        kode = AVVIST_PERSON_IKKE_FUNNET,
        vedTreff = ::Avvist
    ),
    "Er under 18 år"(
        Opplysning.ER_UNDER_18_AAR,
        kode = AVVIST_ALDER,
        vedTreff = ::Avvist
    ),

    "Er over 18 år, er bosatt i Norge etter Folkeregisterloven"(
        Opplysning.ER_OVER_18_AAR,
        Opplysning.BOSATT_ETTER_FREG_LOVEN,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    "Avvist fordi personen ikke er bosatt i Norge i henhold til folkeregisterloven"(
        kode = AVVIST_IKKE_BOSATT_I_NORGE_IHT_FOLKEREGISTERLOVEN,
        vedTreff = ::Avvist
    )
)
