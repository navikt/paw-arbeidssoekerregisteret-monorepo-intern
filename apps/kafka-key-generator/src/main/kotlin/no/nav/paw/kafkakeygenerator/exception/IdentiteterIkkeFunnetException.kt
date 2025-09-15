package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

val IDENTITETER_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("identiteter").error("identiteter-ikke-funnet").build()

class IdentiteterIkkeFunnetException(
    override val message: String = "Fant ingen tilknyttede identiteter"
) : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = IDENTITETER_IKKE_FUNNET_ERROR_TYPE,
    message = message
)