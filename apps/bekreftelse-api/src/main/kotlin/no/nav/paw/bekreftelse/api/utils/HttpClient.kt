package no.nav.paw.bekreftelse.api.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import no.nav.paw.error.exception.ClientResponseException

private fun HttpStatusCode.hasBody(): Boolean {
    return this == HttpStatusCode.BadRequest ||
            this == HttpStatusCode.Forbidden ||
            this == HttpStatusCode.NotFound ||
            this == HttpStatusCode.InternalServerError
}

suspend fun handleError(response: HttpResponse) {
    if (response.status.isSuccess()) {
        return
    } else if (response.status.hasBody()) {
        val error = response.bodyAsText()
        throw ClientResponseException(
            HttpStatusCode.InternalServerError,
            "PAW_HTTP_KLIENT_KALL_FEILET",
            "HTTP-kall feilet med status: ${response.status} og body:\n$error"
        )
    } else {
        throw ClientResponseException(
            HttpStatusCode.InternalServerError,
            "PAW_HTTP_KLIENT_KALL_FEILET",
            "HTTP-kall feilet med status: ${response.status}"
        )
    }
}