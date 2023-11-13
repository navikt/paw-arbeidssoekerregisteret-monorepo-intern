package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet

fun genererNyInternTilstandOgNyeApiTilstander(
    recordKey: Long,
    internTilstandOgHendelse: InternTilstandOgHendelse
): InternTilstandOgApiTilstander {
    val (tilstand, hendelse) = internTilstandOgHendelse
    return when {
        hendelse is Startet -> tilstand.startPeriode(recordKey, hendelse)
        hendelse is Stoppet -> tilstand.avsluttPeriode(hendelse)
        hendelse is SituasjonMottat -> tilstand.situasjonMottatt(recordKey, hendelse)
        else -> throw IllegalStateException("Uventet hendelse: $hendelse")
    }
}

