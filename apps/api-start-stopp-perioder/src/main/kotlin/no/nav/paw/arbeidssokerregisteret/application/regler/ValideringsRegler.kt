package no.nav.paw.arbeidssokerregisteret.application.regler

import no.nav.paw.arbeidssokerregisteret.application.Regel
import no.nav.paw.arbeidssokerregisteret.application.RegelId
import no.nav.paw.arbeidssokerregisteret.application.Regler
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.grunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.invoke
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.skalAvises

object ValideringsRegler : Regler {
    override val regler: List<Regel> = listOf(
        AnsattHarTilgangTilBruker(
            AuthOpplysning.AnsattTilgang,
            vedTreff = ::grunnlagForGodkjenning
        ),
        IkkeAnsattOgIkkeSystemOgForhaandsgodkjent(
            DomeneOpplysning.ErForhaandsgodkjent,
            AuthOpplysning.IkkeAnsatt,
            AuthOpplysning.IkkeSystem,
            vedTreff = ::skalAvises
        ),
        IkkeAnsattOgIkkeSystemOgFeilretting(
            DomeneOpplysning.ErFeilretting,
            AuthOpplysning.IkkeAnsatt,
            AuthOpplysning.IkkeSystem,
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
        ),
        SystemHarIkkeTilgangTilBruker(
            AuthOpplysning.SystemIkkeTilgang,
            vedTreff = ::skalAvises
        ),
        SystemHarTilgangTilBruker(
            AuthOpplysning.SystemTilgang,
            vedTreff = ::grunnlagForGodkjenning
        )
    )

    override val standardRegel: Regel = IkkeTilgang(
        vedTreff = ::skalAvises
    )

}

sealed interface ValideringsRegelId : RegelId

data object IkkeTilgang : ValideringsRegelId {
    override val beskrivelse: String = "Ikke tilgang"
}

data object AnsattIkkeTilgangTilBruker : ValideringsRegelId {
    override val beskrivelse: String = "Ansatt har ikke tilgang til bruker"
}

data object EndreForAnnenBruker : ValideringsRegelId {
    override val beskrivelse: String = "Prøver å endre for en annen bruker"
}

data object EndreEgenBruker : ValideringsRegelId {
    override val beskrivelse: String = "Bruker prøver å endre for seg selv"
}

data object IkkeAnsattOgIkkeSystemOgForhaandsgodkjent : ValideringsRegelId {
    override val beskrivelse: String = "Ikke ansatt eller system har satt forhaandsgodkjenningAvVeileder"
}

data object IkkeAnsattOgIkkeSystemOgFeilretting : ValideringsRegelId {
    override val beskrivelse: String = "Ikke ansatt eller system har satt feilretting"
}

data object UgyldigFeilretting : ValideringsRegelId {
    override val beskrivelse: String = "Feilrettingen er ugyldig"
}

data object AnsattHarTilgangTilBruker : ValideringsRegelId {
    override val beskrivelse: String = "Ansatt har tilgang til bruker"
}

data object SystemHarIkkeTilgangTilBruker : ValideringsRegelId {
    override val beskrivelse: String = "System har ikke tilgang til bruker"
}

data object SystemHarTilgangTilBruker : ValideringsRegelId {
    override val beskrivelse: String = "System har tilgang til bruker"
}
