package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand

data class InternTilstandOgHendelse(val tilstand: Tilstand?, val hendelse: Hendelse)

data class InternTilstandOgApiTilstander(
    val tilstand: Tilstand?,
    val nyePeriodeTilstand: Periode?,
    val nySituasjonTilstand: Situasjon?
)

