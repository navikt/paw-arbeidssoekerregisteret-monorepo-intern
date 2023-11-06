package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.api
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet

fun Tilstand?.avsluttPeriode(hendelse: Stoppet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode == null) throw IllegalStateException("Gjeldene periode er null. Kan ikke avslutte periode.")
    val stoppetPeriode = gjeldenePeriode.copy(avsluttet = metadata(hendelse.metadata))
    return InternTilstandOgApiTilstander(
        tilstand = copy(
            gjeldeneTilstand = GjeldeneTilstand.STOPPET,
            gjeldenePeriode = null,
            forrigePeriode = stoppetPeriode
        ),
        periode = Periode(
            stoppetPeriode.id,
            stoppetPeriode.identitetsnummer,
            stoppetPeriode.startet.api(),
            stoppetPeriode.avsluttet?.api()
        ),
        situasjon = null
    )
}

