package no.nav.paw.bekreftelse.api.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class DataIkkeFunnetForIdException(message: String) :
    ServerResponseException(HttpStatusCode.BadRequest, "PAW_DATA_IKKE_FUNNET_FOR_ID", message)