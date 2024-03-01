package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*

val reglerForInngangIPrioritertRekkefolge: List<Regel<EndeligResultat>> = listOf(
    "Er forhåndsgodkjent av ansatt"(
        Fakta.FORHAANDSGODKJENT_AV_ANSATT,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    "Person ikke funnet"(
        Fakta.PERSON_IKKE_FUNNET,
        kode = AVVIST_PERSON_IKKE_FUNNET,
        vedTreff = ::Avvist
    ),
    "Er under 18 år"(
        Fakta.ER_UNDER_18_AAR,
        kode = AVVIST_ALDER,
        vedTreff = ::Avvist
    ),
    // holder det med bosatt etter Folkeregisterloven så denne kan fjernes?
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.HAR_GYLDIG_OPPHOLDSTILLATELSE,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    //kan v iher fjerne adressegreia
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.BOSATT_ETTER_FREG_LOVEN,
        kode = GODTATT,
        vedTreff = ::OK
    ),
    //fjernes?
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.DNUMMER,
        kode = GODTATT,
        vedTreff = ::OK
    ), //kunn sjekk om bosted ikke er good i Fakta.BOSATT_ETTER_FREG_LOVEN,
    "Bor i utlandet"(
        Fakta.HAR_UTENLANDSK_ADRESSE,
        kode = AVVIST_UTENLANDSK_ADRESSE,
        vedTreff = ::Avvist
    ),
    "Ingen regler funnet for evaluering av forespørsel"(
        kode = AVVIST_UAVKLART,
        vedTreff = ::Uavklart
    )
)
