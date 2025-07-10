package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

val IDENTITET_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("identitet").error("identitet-ikke-funnet").build()

class IdentitetIkkeFunnetException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = IDENTITET_IKKE_FUNNET_ERROR_TYPE,
    message = message
)