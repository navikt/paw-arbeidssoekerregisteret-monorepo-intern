package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.Hendelse
import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet



fun ignorerDuplikatStartOgStopp(recordKey: Long, tilstandOgHendelse: InternTilstandOgHendelse): Boolean {
    val (tilstand, hendelse) = tilstandOgHendelse
    return when (tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>()
        GjeldeneTilstand.STOPPET -> hendelse.erIkke<Stoppet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
    }
}

inline fun <reified A : Hendelse> Hendelse.erIkke(): Boolean = this !is A
inline fun <reified A : Hendelse, reified B : Hendelse> Hendelse.erIkkeEnAv(): Boolean = this !is A && this !is B
