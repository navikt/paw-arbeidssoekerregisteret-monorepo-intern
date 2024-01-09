package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode

class StatusException(
    status: HttpStatusCode,
    beskrivelse: String? = null,
    feilkode: Feilkode
) : ArbeidssoekerException(status, beskrivelse, feilkode)
