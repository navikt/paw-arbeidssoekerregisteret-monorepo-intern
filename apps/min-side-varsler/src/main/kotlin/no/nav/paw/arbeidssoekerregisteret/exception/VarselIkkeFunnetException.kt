package no.nav.paw.arbeidssoekerregisteret.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class VarselIkkeFunnetException(override val message: String) : ServerResponseException(
    status = HttpStatusCode.NotFound,
    type = ErrorType.domain("varsler").error("varsel-ikke-funnet").build(),
    message = message,
)