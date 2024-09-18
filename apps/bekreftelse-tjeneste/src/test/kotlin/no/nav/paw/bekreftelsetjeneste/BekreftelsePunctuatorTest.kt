package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec

class BekreftelsePunctuatorTest : FreeSpec({

    "Tilstand IkkeKlarForUtfylling og bekreftelseTilgjengeligOffset før gjelderTil settes til KlarForUtfylling og sender BekreftelseTilgjengelig hendelse" {

    }

    /*for hver bekreftelse i "klar for utfylling" og gjelderTil passert, sett til "venter på svar" og send hendelse LeveringsFristUtloept

    for hver bekreftelse i "venter på svar" og ingen purring sendt og x tid passert siden frist, send RegisterGracePeriodeGjenstaaendeTid og sett purring sendt timestamp til now()

    for hver bekreftelse i "venter på svar" og grace periode utløpt, send RegisterGracePeriodeUtloept

    for hver periode hvis det er mindre enn x dager til den siste bekreftelse perioden utgår lag ny bekreftelse periode*/
})


