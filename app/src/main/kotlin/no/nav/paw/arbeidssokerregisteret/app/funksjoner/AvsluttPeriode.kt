package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet

fun Tilstand?.avsluttPeriode(hendelse: Avsluttet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode == null) throw IllegalStateException("Gjeldene periode er null. Kan ikke avslutte periode.")
    val stoppetPeriode = gjeldenePeriode.copy(avsluttet = hendelse.metadata)
    return InternTilstandOgApiTilstander(
        tilstand = copy(
            gjeldeneTilstand = GjeldeneTilstand.STOPPET,
            gjeldenePeriode = null,
            forrigePeriode = stoppetPeriode
        ),
        nyePeriodeTilstand = ApiPeriode(
            stoppetPeriode.id,
            stoppetPeriode.identitetsnummer,
            stoppetPeriode.startet.api(),
            stoppetPeriode.avsluttet?.api()
        ),
        nyOpplysningerOmArbeidssoekerTilstand = null
    )
}

