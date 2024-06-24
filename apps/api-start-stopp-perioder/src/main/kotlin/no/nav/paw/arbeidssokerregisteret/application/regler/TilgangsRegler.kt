package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning

val tilgangsReglerIPrioritertRekkefolge: List<Regel> = listOf(
    "Ansatt har tilgang til bruker"(
        AuthOpplysning.AnsattTilgang,
        id = AnsattHarTilgangTilBruker,
        vedTreff = ::ok
    ),
    "Ikke ansatt har satt forhåndsgodkjenningAvVeileder"(
        DomeneOpplysning.ErForhaandsgodkjent,
        AuthOpplysning.IkkeAnsatt,
        id = IkkeAnsattOgForhaandsgodkjentAvAnsatt,
        vedTreff = ::problem
    ),
    "Bruker prøver å endre for seg selv"(
        AuthOpplysning.SammeSomInnloggetBruker,
        AuthOpplysning.IkkeAnsatt,
        id = EndreEgenBruker,
        vedTreff = ::ok
    ),
    "Prøver å endre for en annen bruker"(
        AuthOpplysning.IkkeSammeSomInnloggerBruker,
        id = EndreForAnnenBruker,
        vedTreff = ::problem
    ),
    "Ansatt har ikke tilgang til bruker"(
        AuthOpplysning.AnsattIkkeTilgang,
        id = AnsattIkkeTilgangTilBruker,
        vedTreff = ::problem
    ),
    "Ikke tilgang"(
        id = IkkeTilgang,
        vedTreff = ::problem
    )
)

sealed interface AuthRegelId: RegelId

data object IkkeTilgang : AuthRegelId {
    override val id: String = "IKKE_TILGANG"
}

data object AnsattIkkeTilgangTilBruker : AuthRegelId {
    override val id: String = "ANSATT_IKKE_TILGANG_TIL_BRUKER"
}

data object EndreForAnnenBruker : AuthRegelId {
    override val id: String = "ENDRE_FOR_ANNEN_BRUKER"
}

data object EndreEgenBruker : AuthRegelId {
    override val id: String = "ENDRE_EGEN_BRUKER"
}

data object IkkeAnsattOgForhaandsgodkjentAvAnsatt : AuthRegelId {
    override val id: String = "IKKE_ANSATT_OG_FORHAANDSGODKJENT_AV_ANSATT"
}

data object AnsattHarTilgangTilBruker : AuthRegelId {
    override val id: String = "ANSATT_HAR_TILGANG_TIL_BRUKER"
}


