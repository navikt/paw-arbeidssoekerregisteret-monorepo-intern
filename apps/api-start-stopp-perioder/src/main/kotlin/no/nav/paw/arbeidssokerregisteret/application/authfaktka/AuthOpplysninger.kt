package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning


sealed interface AuthOpplysning: Opplysning {

    data object IkkeSammeSomInnloggerBruker : AuthOpplysning {
        override val id = "IKKE_SAMME_SOM_INNLOGGER_BRUKER"
        override val beskrivelse = "Start/stopp av periode er ikke på samme bruker som er innlogget"
    }

    data object SammeSomInnloggetBruker : AuthOpplysning {
        override val id = "SAMME_SOM_INNLOGGET_BRUKER"
        override val beskrivelse = "Start/stopp av periode er på samme bruker som er innlogget"
    }

    data object TokenXPidIkkeFunnet : AuthOpplysning {
        override val id = "TOKENX_PID_IKKE_FUNNET"
        override val beskrivelse =
            "Innlogget bruker er ikke en logget inn via TOKENX med PID(dvs ikke sluttbruker via ID-Porten)"
    }

    data object AnsattIkkeTilgang : AuthOpplysning {
        override val id = "ANSATT_IKKE_TILGANG"
        override val beskrivelse =
            "Innlogget bruker er en NAV-ansatt uten tilgang til bruker som start/stopp av periode utføres på"
    }

    data object AnsattTilgang : AuthOpplysning {
        override val id = "ANSATT_TILGANG"
        override val beskrivelse =
            "Innlogget bruker er en NAV-ansatt med tilgang til bruker som start/stopp av periode utføres på"
    }

    data object IkkeAnsatt : AuthOpplysning {
        override val id = "IKKE_ANSATT"
        override val beskrivelse = "Innlogget bruker er ikke en NAV-ansatt"
    }
}