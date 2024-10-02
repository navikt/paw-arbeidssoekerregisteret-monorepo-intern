package no.nav.paw.bekreftelse.api.consumer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.isSuccess
import no.nav.paw.bekreftelse.api.exception.ProblemDetailsException
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest
import no.nav.paw.error.model.ProblemDetails

class BekreftelseHttpConsumer(private val httpClient: HttpClient) {

    suspend fun finnTilgjengeligBekreftelser(
        host: String,
        bearerToken: String,
        request: TilgjengeligeBekreftelserRequest
    ): TilgjengeligBekreftelserResponse {
        val url = "http://$host/api/v1/tilgjengelige-bekreftelser"
        val response = httpClient.post(url) {
            bearerAuth(bearerToken)
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            setBody(request)
        }
        if (response.status.isSuccess()) {
            return response.body()
        } else {
            val problemDetails = response.body<ProblemDetails>()
            throw ProblemDetailsException(problemDetails)
        }
    }

    suspend fun sendBekreftelse(
        host: String,
        bearerToken: String,
        request: BekreftelseRequest,
    ) {
        val url = "http://$host/api/v1/bekreftelse"
        val response = httpClient.post(url) {
            bearerAuth(bearerToken)
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            setBody(request)
        }
        if (response.status.isSuccess()) {
            val problemDetails = response.body<ProblemDetails>()
            throw ProblemDetailsException(problemDetails)
        }
    }
}