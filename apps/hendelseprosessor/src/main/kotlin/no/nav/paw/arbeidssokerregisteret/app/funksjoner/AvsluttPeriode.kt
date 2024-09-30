package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet

fun FunctionContext<TilstandV1?, Long>.avsluttPeriode(hendelse: Avsluttet): InternTilstandOgApiTilstander {
    if (tilstand?.gjeldenePeriode == null) throw IllegalStateException("Gjeldene periode er null. Kan ikke avslutte periode.")
    val stoppetPeriode = tilstand.gjeldenePeriode.copy(
        avsluttet = hendelse.metadata,
        avsluttetVedOffset = scope.offset
    )
    return InternTilstandOgApiTilstander(
        id = scope.id,
        tilstand = tilstand.copy(
            gjeldeneTilstand = GjeldeneTilstand.AVSLUTTET,
            gjeldenePeriode = null,
            forrigePeriode = stoppetPeriode,
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            alleIdentitetsnummer = tilstand.alleIdentitetsnummer + hendelse.identitetsnummer,
            hendelseScope = scope
        ),
        nyPeriodeTilstand = ApiPeriode(
            stoppetPeriode.id,
            stoppetPeriode.identitetsnummer,
            stoppetPeriode.startet.api(),
            stoppetPeriode.avsluttet?.api()
        ),
        nyOpplysningerOmArbeidssoekerTilstand = null
    )
}

