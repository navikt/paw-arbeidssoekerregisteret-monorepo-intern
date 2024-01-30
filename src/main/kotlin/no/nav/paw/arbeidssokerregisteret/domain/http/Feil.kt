package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.evaluering.Attributt
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode

data class Feil(
    val melding: String,
    val kode: Feilkode,
    val aarasakTilAvvisning: AarsakTilAvvisning? = null
)

data class AarsakTilAvvisning(
    val beskrivelse: String,
    val detaljer: Set<Attributt>
)
