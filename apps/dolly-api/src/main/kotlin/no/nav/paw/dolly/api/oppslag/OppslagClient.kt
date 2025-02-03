package no.nav.paw.dolly.api.oppslag

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.dolly.api.config.OppslagClientConfig

data class OppslagResponse(
    val periodeId: java.util.UUID,
    val startet: MetadataResponse,
    val avsluttet: MetadataResponse? = null,
    val opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerAggregertResponse>? = null,
    val bekreftelser: List<BekreftelseResponse>? = null
)

interface OppslagClient {
    suspend fun hentAggregerteArbeidssoekerperioder(identitetsnummer: String): OppslagResponse?
}

fun oppslagClient(config: OppslagClientConfig, m2mTokenFactory: () -> String): OppslagClient =
    OppslagClientImpl(config.url, m2mTokenFactory)

class OppslagClientImpl(
    private val url: String,
    private val getAccessToken: () -> String
) : OppslagClient {
    private val httpClient = createHttpClient()
    override suspend fun hentAggregerteArbeidssoekerperioder(identitetsnummer: String): OppslagResponse? {
        httpClient.post(url) {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(identitetsnummer)
        }.let { response ->
            return when (response.status) {
                io.ktor.http.HttpStatusCode.OK -> {
                    response.body<OppslagResponse>()
                }

                io.ktor.http.HttpStatusCode.NotFound -> null
                else -> {
                    throw Exception("Kunne ikke hente aggregerte arbeidssoekerperioder, http_status=${response.status}, melding=${response.body<String>()}")
                }
            }
        }
    }
}