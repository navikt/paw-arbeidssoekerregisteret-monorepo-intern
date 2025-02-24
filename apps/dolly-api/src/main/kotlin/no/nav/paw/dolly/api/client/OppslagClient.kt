package no.nav.paw.dolly.api.client

import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.dolly.api.config.OppslagClientConfig
import no.nav.paw.dolly.api.model.OppslagRequest
import no.nav.paw.dolly.api.model.OppslagResponse
import no.nav.paw.serialization.jackson.configureJackson

interface OppslagClient {
    suspend fun hentAggregerteArbeidssoekerperioder(identitetsnummer: String): List<OppslagResponse>?
}

fun oppslagClient(config: OppslagClientConfig, m2mTokenFactory: () -> String): OppslagClient =
    OppslagClientImpl(config.url, m2mTokenFactory)

class OppslagClientImpl(
    private val url: String,
    private val getAccessToken: () -> String
) : OppslagClient {
    private val httpClient = createHttpClient {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
        install(HttpRequestRetry) {
            maxRetries = 2
            retryIf { _, response ->
                response.status == HttpStatusCode.NotFound
            }
        }
    }

    override suspend fun hentAggregerteArbeidssoekerperioder(identitetsnummer: String): List<OppslagResponse>? {
        httpClient.post(url) {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(OppslagRequest(identitetsnummer))
        }.let { response ->
            return when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<List<OppslagResponse>>()
                }

                HttpStatusCode.NotFound -> null
                else -> {
                    throw Exception("Kunne ikke hente aggregerte arbeidssoekerperioder, http_status=${response.status}, melding=${response.body<String>()}")
                }
            }
        }
    }
}