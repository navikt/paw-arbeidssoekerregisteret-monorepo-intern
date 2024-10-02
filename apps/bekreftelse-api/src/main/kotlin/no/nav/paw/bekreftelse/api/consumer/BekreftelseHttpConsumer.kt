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
import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelserResponse
import no.nav.paw.bekreftelse.api.model.TilgjengeligeBekreftelserRequest

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
        return response.body()
    }

    suspend fun sendBekreftelse(
        host: String,
        bearerToken: String,
        request: BekreftelseRequest,
    ) {
        val url = "http://$host/api/v1/bekreftelse"
        httpClient.post(url) {
            bearerAuth(bearerToken)
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            setBody(request)
        }
    }
}