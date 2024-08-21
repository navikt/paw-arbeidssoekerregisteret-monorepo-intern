package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning

val tilgangsReglerIPrioritertRekkefolge: List<Regel> = listOf(
    AnsattHarTilgangTilBruker(
        AuthOpplysning.AnsattTilgang,
        vedTreff = ::ok
    ),
    IkkeAnsattOgForhaandsgodkjentAvAnsatt(
        DomeneOpplysning.ErForhaandsgodkjent,
        AuthOpplysning.IkkeAnsatt,
        vedTreff = ::problem
    ),
    EndreEgenBruker(
        AuthOpplysning.SammeSomInnloggetBruker,
        AuthOpplysning.IkkeAnsatt,
        vedTreff = ::ok
    ),
    EndreForAnnenBruker(
        AuthOpplysning.IkkeSammeSomInnloggerBruker,
        vedTreff = ::problem
    ),
    AnsattIkkeTilgangTilBruker(
        AuthOpplysning.AnsattIkkeTilgang,
        vedTreff = ::problem
    ),
    IkkeTilgang(
        vedTreff = ::problem
    )
)

sealed interface AuthRegelId: RegelId

data object IkkeTilgang : AuthRegelId {
    override val beskrivelse: String = "Ikke tilgang"
}

data object AnsattIkkeTilgangTilBruker : AuthRegelId {
    override val beskrivelse: String = "Ansatt har ikke tilgang til bruker"
}

data object EndreForAnnenBruker : AuthRegelId {
    override val beskrivelse: String = "Prøver å endre for en annen bruker"
}

data object EndreEgenBruker : AuthRegelId {
    override val beskrivelse: String = "Bruker prøver å endre for seg selv"
}

data object IkkeAnsattOgForhaandsgodkjentAvAnsatt : AuthRegelId {
    override val beskrivelse: String = "Ikke ansatt har satt forhaandsgodkjenningAvVeileder"
}

data object AnsattHarTilgangTilBruker : AuthRegelId {
    override val beskrivelse: String = "Ansatt har tilgang til bruker"
}


