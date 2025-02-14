package no.nav.paw.arbeidssoeker.synk.consumer

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoeker.synk.model.OpprettPeriodeRequest
import no.nav.paw.client.factory.createHttpClient

class InngangHttpConsumer(
    baseUrl: String,
    private val httpClient: HttpClient = createHttpClient(),
    private val getAccessToken: () -> String
) {
    private val periodeUrl = "$baseUrl/api/v2/arbeidssoker/periode"

    fun opprettPeriode(request: OpprettPeriodeRequest): HttpResponse = runBlocking {
        httpClient.put(periodeUrl) {
            bearerAuth(getAccessToken())
            setJsonBody(request)
        }
    }

    private inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}