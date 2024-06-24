package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*

class StatusException(
    status: HttpStatusCode,
    beskrivelse: String? = null,
    feilkode: InternFeilkode
) : ArbeidssoekerException(status, beskrivelse, feilkode)
