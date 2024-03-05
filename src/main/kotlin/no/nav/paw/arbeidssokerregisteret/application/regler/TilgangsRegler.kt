package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*

val tilgangsReglerIPrioritertRekkefolge: List<Regel<TilgangskontrollResultat>> = listOf(
    "Ansatt har tilgang til bruker"(
        Opplysning.ANSATT_TILGANG,
        id = RegelId.ANSATT_HAR_TILGANG_TIL_BRUKER,
        vedTreff = ::TilgangOK
    ),
    "Bruker prøver å endre for seg selv"(
        Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
        Opplysning.IKKE_ANSATT,
        id = RegelId.ENDRE_EGEN_BRUKER,
        vedTreff = ::TilgangOK
    ),
    "Prøver å endre for en annen bruker"(
        Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
        id = RegelId.ENDRE_FOR_ANNEN_BRUKER,
        vedTreff = ::IkkeTilgang
    ),
    "Ansatt har ikke tilgang til bruker"(
        Opplysning.ANSATT_IKKE_TILGANG,
        id = RegelId.ANSATT_IKKE_TILGANG_TIL_BRUKER,
        vedTreff = ::IkkeTilgang
    ),
    "Ikke tilgang"(
        id = RegelId.IKKE_TILGANG,
        vedTreff = ::IkkeTilgang
    )
)
