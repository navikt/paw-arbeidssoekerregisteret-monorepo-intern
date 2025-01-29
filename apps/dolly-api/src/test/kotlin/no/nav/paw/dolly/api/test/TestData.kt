package no.nav.paw.dolly.api.test

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest

inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    headers {
        append(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    setBody(body)
}

object TestData {

    fun nyArbeidssoekerregistreringRequest() =
        ArbeidssoekerregistreringRequest(
            identitetsnummer = "12345678911",
        )

}