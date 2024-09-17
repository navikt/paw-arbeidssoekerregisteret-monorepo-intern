package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException

class ArbeidsoekerIdIkkeFunnetException(message: String) :
    ServerResponseException(HttpStatusCode.Forbidden, "ARBEIDSOEKER_ID_IKKE_FUNNET", message, null) {
}