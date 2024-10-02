package no.nav.paw.bekreftelse.api.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class SystemfeilException(message: String) :
    ServerResponseException(HttpStatusCode.InternalServerError, "PAW_SYSTEMFEIL", message, null)