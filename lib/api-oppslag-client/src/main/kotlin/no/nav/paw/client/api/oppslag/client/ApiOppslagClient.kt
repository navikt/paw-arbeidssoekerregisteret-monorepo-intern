package no.nav.paw.client.api.oppslag.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.client.api.oppslag.exception.PerioderOppslagResponseException
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeRequest
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.logging.logger.buildNamedLogger

class ApiOppslagClient(
    private val baseUrl: String,
    private val getAccessToken: () -> String
) {
    private val logger = buildNamedLogger("http.periode")
    private val httpClient: HttpClient = createHttpClient()

    suspend fun findPerioder(identitet: String): List<ArbeidssoekerperiodeResponse> {
        logger.debug("Henter arbeidssoekerperioder fra API Oppslag")
        val response = httpClient.post("$baseUrl/api/v1/veileder/arbeidssoekerperioder") {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(ArbeidssoekerperiodeRequest(identitet))
        }
        return response.let {
            when (it.status) {
                HttpStatusCode.OK -> it.body<List<ArbeidssoekerperiodeResponse>>()
                else -> {
                    val body = it.body<String>()
                    throw PerioderOppslagResponseException(it.status, "Henting av perioder feilet. body=$body")
                }
            }
        }
    }
}