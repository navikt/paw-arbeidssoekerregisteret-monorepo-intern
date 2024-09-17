package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class BearerTokenManglerException(message: String) :
    ServerResponseException(HttpStatusCode.Unauthorized, "BEARER_TOKEN_MANGLER", message, null) {
}