package no.nav.paw.arbeidssokerregisteret.application.authfaktka

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Effect
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning


sealed interface AuthOpplysning : Opplysning {

    data object IkkeSammeSomInnloggerBruker : AuthOpplysning, Effect.Negative {
        override val id = "IKKE_SAMME_SOM_INNLOGGER_BRUKER"
        override val beskrivelse = "Start/stopp av periode er ikke på samme bruker som er innlogget"
    }

    data object SammeSomInnloggetBruker : AuthOpplysning, Effect.Positive {
        override val id = "SAMME_SOM_INNLOGGET_BRUKER"
        override val beskrivelse = "Start/stopp av periode er på samme bruker som er innlogget"
    }

    data object TokenXPidIkkeFunnet : AuthOpplysning, Effect.Neutral {
        override val id = "TOKENX_PID_IKKE_FUNNET"
        override val beskrivelse =
            "Innlogget bruker er ikke en logget inn via TOKENX med PID(dvs ikke sluttbruker via ID-Porten)"
    }

    data object AnsattIkkeTilgang : AuthOpplysning, Effect.Negative {
        override val id = "ANSATT_IKKE_TILGANG"
        override val beskrivelse =
            "Innlogget bruker er en NAV-ansatt uten tilgang til bruker som start/stopp av periode utføres på"
    }

    data object AnsattTilgang : AuthOpplysning, Effect.Positive {
        override val id = "ANSATT_TILGANG"
        override val beskrivelse =
            "Innlogget bruker er en NAV-ansatt med tilgang til bruker som start/stopp av periode utføres på"
    }

    data object IkkeAnsatt : AuthOpplysning, Effect.Neutral {
        override val id = "IKKE_ANSATT"
        override val beskrivelse = "Innlogget bruker er ikke en NAV-ansatt"
    }

    data object SystemIkkeTilgang : AuthOpplysning, Effect.Negative {
        override val id = "SYSTEM_IKKE_TILGANG"
        override val beskrivelse =
            "System uten tilgang til å utføre start/stopp av perioder"
    }

    data object SystemTilgang : AuthOpplysning, Effect.Positive {
        override val id = "SYSTEM_TILGANG"
        override val beskrivelse =
            "System med tilgang til å utføre start/stopp av perioder"
    }

    data object IkkeSystem : AuthOpplysning, Effect.Neutral {
        override val id = "IKKE_SYSTEM"
        override val beskrivelse = "Ikke system"
    }
}