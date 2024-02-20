package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt



fun ignorerDuplikatStartOgStopp(
    @Suppress("UNUSED_PARAMETER") recordKey: Long,
    tilstandOgHendelse: InternTilstandOgHendelse
): Boolean {
    val (_, tilstand, hendelse) = tilstandOgHendelse
    return when (tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>()
        GjeldeneTilstand.STOPPET -> hendelse.erIkke<Avsluttet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkke<Avsluttet>()
    }
}

inline fun <reified A : StreamHendelse> StreamHendelse.erIkke(): Boolean = this !is A