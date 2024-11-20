package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning

object TilgangsRegler: Regler {
    override val regler: List<Regel> = listOf(
        AnsattHarTilgangTilBruker(
            AuthOpplysning.AnsattTilgang,
            vedTreff = ::grunnlagForGodkjenning
        ),
        IkkeAnsattOgForhaandsgodkjentAvAnsatt(
            DomeneOpplysning.ErForhaandsgodkjent,
            AuthOpplysning.IkkeAnsatt,
            vedTreff = ::skalAvises
        ),
        IkkeAnsattOgFeilretting(
            DomeneOpplysning.ErFeilretting,
            AuthOpplysning.IkkeAnsatt,
            vedTreff = ::skalAvises
        ),
        UgyldigFeilretting(
            DomeneOpplysning.UgyldigFeilretting,
            vedTreff = ::skalAvises
        ),
        EndreEgenBruker(
            AuthOpplysning.SammeSomInnloggetBruker,
            AuthOpplysning.IkkeAnsatt,
            vedTreff = ::grunnlagForGodkjenning
        ),
        EndreForAnnenBruker(
            AuthOpplysning.IkkeSammeSomInnloggerBruker,
            vedTreff = ::skalAvises
        ),
        AnsattIkkeTilgangTilBruker(
            AuthOpplysning.AnsattIkkeTilgang,
            vedTreff = ::skalAvises
        )
    )

    override val standardRegel: Regel = IkkeTilgang(
        vedTreff = ::skalAvises
    )

}

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

data object IkkeAnsattOgFeilretting : AuthRegelId {
    override val beskrivelse: String = "Ikke ansatt har satt feilretting"
}

data object UgyldigFeilretting : AuthRegelId {
    override val beskrivelse: String = "Feilrettingen er ugyldig"
}

data object AnsattHarTilgangTilBruker : AuthRegelId {
    override val beskrivelse: String = "Ansatt har tilgang til bruker"
}
