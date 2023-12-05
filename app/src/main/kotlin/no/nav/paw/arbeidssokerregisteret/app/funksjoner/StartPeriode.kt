package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode

context (RecordScope<Long>)
fun Tilstand?.startPeriode(hendelse: Startet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode != null) throw IllegalStateException("Gjeldene periode er ikke null. Kan ikke starte ny periode.")
    val startetPeriode = Periode(
        id = UUID.randomUUID(),
        identitetsnummer = hendelse.identitetsnummer,
        startet = hendelse.metadata,
        avsluttet = null
    )
    val tilstand: Tilstand = this?.copy(
        gjeldeneTilstand = GjeldeneTilstand.STARTET,
        gjeldenePeriode = startetPeriode
    )
        ?: Tilstand(
            recordScope = currentScope(),
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            allIdentitetsnummer = setOf(hendelse.identitetsnummer),
            gjeldeneTilstand = GjeldeneTilstand.STARTET,
            gjeldenePeriode = startetPeriode,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )
    return InternTilstandOgApiTilstander(
        recordScope = currentScope(),
        tilstand = tilstand,
        nyOpplysningerOmArbeidssoekerTilstand = null,
        nyePeriodeTilstand = ApiPeriode(
            startetPeriode.id,
            startetPeriode.identitetsnummer,
            startetPeriode.startet.api(),
            startetPeriode.avsluttet?.api()
        )
    )
}