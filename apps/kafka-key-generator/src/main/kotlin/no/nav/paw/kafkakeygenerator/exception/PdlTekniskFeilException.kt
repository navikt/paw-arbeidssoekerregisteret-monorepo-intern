package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ErrorType

val PDL_TEKNISK_FEIL_ERROR_TYPE = ErrorType.domain("identiteter").error("pdl-teknisk-feil").build()

class PdlTekniskFeilException(
    override val message: String = "Mottok teknisk feil fra PDL"
) : ClientResponseException(
    status = HttpStatusCode.InternalServerError,
    type = PDL_TEKNISK_FEIL_ERROR_TYPE,
    message = message
)