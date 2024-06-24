package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.arbeidssokerregisteret.application.authfaktka.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning

fun authOpplysningTilHendelseOpplysning(opplysning: AuthOpplysning): Opplysning =
    when (opplysning) {
        IkkeSammeSomInnloggerBruker -> Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER
        SammeSomInnloggetBruker -> Opplysning.SAMME_SOM_INNLOGGET_BRUKER
        TokenXPidIkkeFunnet -> Opplysning.TOKENX_PID_IKKE_FUNNET
        AnsattIkkeTilgang -> Opplysning.ANSATT_IKKE_TILGANG
        AnsattTilgang -> Opplysning.ANSATT_TILGANG
        IkkeAnsatt -> Opplysning.IKKE_ANSATT
    }
