package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.AnsattIkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.AnsattTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeAnsatt
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeSystem
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeSammeSomInnloggerBruker
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SystemIkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SystemTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.SammeSomInnloggetBruker
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.TokenXPidIkkeFunnet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning

fun authOpplysningTilHendelseOpplysning(opplysning: AuthOpplysning): Opplysning =
    when (opplysning) {
        IkkeSammeSomInnloggerBruker -> Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        SammeSomInnloggetBruker -> Opplysning.SAMME_SOM_INNLOGGET_BRUKER
        TokenXPidIkkeFunnet -> Opplysning.TOKENX_PID_IKKE_FUNNET
        AnsattIkkeTilgang -> Opplysning.ANSATT_IKKE_TILGANG
        AnsattTilgang -> Opplysning.ANSATT_TILGANG
        IkkeAnsatt -> Opplysning.IKKE_ANSATT
        SystemIkkeTilgang -> Opplysning.SYSTEM_IKKE_TILGANG
        SystemTilgang -> Opplysning.SYSTEM_TILGANG
        IkkeSystem -> Opplysning.IKKE_SYSTEM
    }
