package no.nav.paw.bekreftelse.api.exception

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class DataTilhoererIkkeBrukerException(message: String) :
    ServerResponseException(HttpStatusCode.BadRequest, "PAW_DATA_TILHOERER_IKKE_BRUKER", message, null)