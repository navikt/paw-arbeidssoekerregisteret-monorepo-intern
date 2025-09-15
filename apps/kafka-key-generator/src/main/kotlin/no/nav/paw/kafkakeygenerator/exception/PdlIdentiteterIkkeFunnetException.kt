package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

val PDL_IKKE_FUNNET_ERROR_TYPE = ErrorType.domain("identiteter").error("pdl-identiteter-ikke-funnet").build()

class PdlIdentiteterIkkeFunnetException(
    override val message: String = "Fant ingen identiteter i PDL"
) : ClientResponseException(
    status = HttpStatusCode.NotFound,
    type = PDL_IKKE_FUNNET_ERROR_TYPE,
    message = message
)