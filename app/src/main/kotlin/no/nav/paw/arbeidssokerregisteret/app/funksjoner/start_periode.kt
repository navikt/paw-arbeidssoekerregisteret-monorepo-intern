package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Periode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.api
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode

fun Tilstand?.startPeriode(recordKey: Long, hendelse: Startet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode != null) throw IllegalStateException("Gjeldene periode er ikke null. Kan ikke starte ny periode.")
    val startetPeriode = Periode(
        id = UUID.randomUUID(),
        identitetsnummer = hendelse.identitetsnummer,
        startet = metadata(hendelse.metadata),
        avsluttet = null
    )
    val tilstand: Tilstand = if (this == null) {
        Tilstand(
            kafkaKey = recordKey,
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            allIdentitetsnummer = setOf(hendelse.identitetsnummer),
            gjeldeneTilstand = GjeldeneTilstand.STARTET,
            gjeldenePeriode = startetPeriode,
            forrigePeriode = null,
            sisteSituasjon = null,
            forrigeSituasjon = null
        )
    } else {
        copy(
            gjeldeneTilstand = GjeldeneTilstand.STARTET,
            gjeldenePeriode = startetPeriode
        )
    }
    return InternTilstandOgApiTilstander(
        tilstand = tilstand,
        situasjon = null,
        periode = ApiPeriode(
            startetPeriode.id,
            startetPeriode.identitetsnummer,
            startetPeriode.startet.api(),
            startetPeriode.avsluttet?.api()
        )
    )
}