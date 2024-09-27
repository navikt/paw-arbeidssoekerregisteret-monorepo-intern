package no.nav.paw.bekreftelse.api.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class DataIkkeFunnetException(message: String) :
    ServerResponseException(HttpStatusCode.BadRequest, "PAW_DATA_IKKE_FUNNET", message, null)