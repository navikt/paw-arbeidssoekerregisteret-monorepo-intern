package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.application.EksternRegelId
import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode

data class Feil(
    val melding: String,
    val feilKode: Feilkode,
    val aarsakTilAvvisning: AarsakTilAvvisning? = null
)

data class AarsakTilAvvisning(
    val beskrivelse: String,
    val regel: EksternRegelId,
    val detaljer: Set<Opplysning>
)
