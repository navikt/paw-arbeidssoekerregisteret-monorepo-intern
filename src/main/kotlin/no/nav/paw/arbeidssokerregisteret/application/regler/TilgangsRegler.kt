package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*

val tilgangsReglerIPrioritertRekkefolge: List<Regel<TilgangskontrollResultat>> = listOf(
    "Ansatt har tilgang til bruker"(
        Opplysning.ANSATT_TILGANG,
        kode = MIDLERTIDIG_INTERNT_RESULTAT,
        vedTreff = ::TilgangOK
    ),
    "Bruker prøver å endre for seg selv"(
        Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
        Opplysning.IKKE_ANSATT,
        kode = MIDLERTIDIG_INTERNT_RESULTAT,
        vedTreff = ::TilgangOK
    ),
    "Prøver å endre for en annen bruker"(
        Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
        kode = IKKE_TILGANG_ANNEN_BRUKER,
        vedTreff = ::IkkeTilgang
    ),
    "Ansatt har ikke tilgang til bruker"(
        Opplysning.ANSATT_IKKE_TILGANG,
        kode = IKKE_TILGANG_ANSATT_IKKE_TILGANG_TIL_BRUKER,
        vedTreff = ::IkkeTilgang
    ),
    "Ikke tilgang"(
        kode = IKKE_TILGANG,
        vedTreff = ::IkkeTilgang
    )
)
