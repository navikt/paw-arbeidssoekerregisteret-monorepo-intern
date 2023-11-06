package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.Hendelse
import no.nav.paw.arbeidssokerregisteret.app.erIkke
import no.nav.paw.arbeidssokerregisteret.app.erIkkeEnAv
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet

fun ignorerDuplikatStartOgStopp(tilstand: Tilstand?, hendelse: Hendelse): Boolean =
    when(tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>()
        GjeldeneTilstand.STOPPET -> hendelse.erIkke<Stoppet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
    }
