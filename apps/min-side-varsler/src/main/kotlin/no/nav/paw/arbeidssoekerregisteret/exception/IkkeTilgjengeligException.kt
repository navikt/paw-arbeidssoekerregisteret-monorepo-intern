package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class IkkeTilgjengeligException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.ServiceUnavailable,
    type = ErrorType.domain("varsler").error("ikke-tilgjengelig").build(),
    message = message
)