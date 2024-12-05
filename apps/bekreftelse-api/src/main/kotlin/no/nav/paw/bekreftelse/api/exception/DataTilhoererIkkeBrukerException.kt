package no.nav.paw.bekreftelse.api.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType

class DataTilhoererIkkeBrukerException(message: String) :
    ServerResponseException(
        HttpStatusCode.BadRequest,
        ErrorType.domain("bekreftelse").error("tilhører-ikke-bruker").build(),
        message
    )