package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*

class StatusException(
    status: HttpStatusCode,
    description: String? = null,
    errorCode: ErrorCode
) : ArbeidssoekerException(status, description, errorCode)
