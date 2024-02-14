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
    // holder det med bosatt etter Folkeregisterloven så denne kan fjernes?
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Opplysning.ER_OVER_18_AAR,
        Opplysning.HAR_NORSK_ADRESSE,
        Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    //kan v iher fjerne adressegreia
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Opplysning.ER_OVER_18_AAR,
        Opplysning.HAR_NORSK_ADRESSE,
        Opplysning.BOSATT_ETTER_FREG_LOVEN,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    //fjernes?
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Opplysning.ER_OVER_18_AAR,
        Opplysning.HAR_NORSK_ADRESSE,
        Opplysning.DNUMMER,
        kode = GODTATT,
        vedTreff = ::OK
    ), //kunn sjekk om bosted ikke er good i Fakta.BOSATT_ETTER_FREG_LOVEN,
    "Bor i utlandet"(
        Opplysning.HAR_UTENLANDSK_ADRESSE,
        kode = AVVIST_UTENLANDSK_ADRESSE,
        vedTreff = ::Avvist
    ),
    "Ingen regler funnet for evaluering av forespørsel"(
        kode = AVVIST_UAVKLART,
        vedTreff = ::Uavklart
    )
)
