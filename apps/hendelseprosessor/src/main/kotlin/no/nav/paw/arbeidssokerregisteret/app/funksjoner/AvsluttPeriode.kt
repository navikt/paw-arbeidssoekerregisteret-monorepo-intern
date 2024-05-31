package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import no.nav.paw.arbeidssokerregisteret.app.tilstand.api
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet

context (HendelseScope<Long>)
fun TilstandV1?.avsluttPeriode(hendelse: Avsluttet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode == null) throw IllegalStateException("Gjeldene periode er null. Kan ikke avslutte periode.")
    val stoppetPeriode = gjeldenePeriode.copy(
        avsluttet = hendelse.metadata,
        avsluttetVedOffset = currentScope().offset
    )
    return InternTilstandOgApiTilstander(
        id = id,
        tilstand = copy(
            gjeldeneTilstand = GjeldeneTilstand.AVSLUTTET,
            gjeldenePeriode = null,
            forrigePeriode = stoppetPeriode,
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            alleIdentitetsnummer = alleIdentitetsnummer + hendelse.identitetsnummer,
            hendelseScope = currentScope()
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

