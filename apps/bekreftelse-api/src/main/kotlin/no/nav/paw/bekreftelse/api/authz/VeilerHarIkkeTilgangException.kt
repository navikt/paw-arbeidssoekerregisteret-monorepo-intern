package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class VeilerHarIkkeTilgangException(message: String) :
    ServerResponseException(HttpStatusCode.Forbidden, "VEILEDER_HAR_IKKE_TILGANG", message, null) {
}