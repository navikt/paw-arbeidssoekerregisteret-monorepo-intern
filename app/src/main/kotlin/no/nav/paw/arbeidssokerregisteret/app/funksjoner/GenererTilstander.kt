package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet

fun genererNyInternTilstandOgNyeApiTilstander(
    recordKey: Long,
    internTilstandOgHendelse: InternTilstandOgHendelse
): InternTilstandOgApiTilstander {
    val (tilstand, hendelse) = internTilstandOgHendelse
    return when (hendelse) {
        is Startet -> tilstand.startPeriode(recordKey, hendelse)
        is Avsluttet -> tilstand.avsluttPeriode(hendelse)
        is OpplysningerOmArbeidssoekerMottatt -> tilstand.opplysningerOmArbeidssoekerMottatt(recordKey, hendelse)
        else -> throw IllegalStateException("Uventet hendelse: $hendelse")
    }
}

