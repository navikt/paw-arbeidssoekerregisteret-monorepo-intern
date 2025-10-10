package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

val PDL_UKJENT_IDENTITET_ERROR_TYPE = ErrorType
    .team("pdl")
    .domain("identiteter")
    .error("ukjent-identitet")
    .build()

class PdlUkjentIdentitetException(
    override val message: String = "Ukjent identitet"
) : ClientResponseException(
    status = HttpStatusCode.NotFound,
    type = PDL_UKJENT_IDENTITET_ERROR_TYPE,
    message = message
)