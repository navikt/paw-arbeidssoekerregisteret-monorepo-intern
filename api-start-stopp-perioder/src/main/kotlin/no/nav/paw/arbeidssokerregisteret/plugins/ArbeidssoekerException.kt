package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode

abstract class ArbeidssoekerException(
    val status: HttpStatusCode,
    description: String? = null,
    val feilkode: Feilkode,
    causedBy: Throwable? = null
) : Exception("Request failed with status: $status. Description: $description. ErrorCode: $feilkode", causedBy)

