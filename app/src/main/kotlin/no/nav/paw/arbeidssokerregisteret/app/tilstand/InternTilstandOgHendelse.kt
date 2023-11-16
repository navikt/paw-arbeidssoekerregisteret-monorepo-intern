package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse

data class InternTilstandOgHendelse(val tilstand: Tilstand?, val hendelse: StreamHendelse)

data class InternTilstandOgApiTilstander(
    val tilstand: Tilstand?,
    val nyePeriodeTilstand: Periode?,
    val nySituasjonTilstand: Situasjon?
)

