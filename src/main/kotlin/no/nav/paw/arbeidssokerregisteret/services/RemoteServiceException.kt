package no.nav.paw.arbeidssokerregisteret.services

import io.ktor.http.*
import no.nav.paw.arbeidssokerregisteret.plugins.ArbeidssoekerException
import no.nav.paw.arbeidssokerregisteret.plugins.ErrorCode

class RemoteServiceException(
    description: String? = null,
    errorCode: ErrorCode,
    causedBy: Throwable? = null
) : ArbeidssoekerException(HttpStatusCode.FailedDependency, description, errorCode, causedBy)
