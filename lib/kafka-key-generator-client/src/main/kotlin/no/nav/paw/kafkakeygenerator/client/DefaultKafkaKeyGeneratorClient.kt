package no.nav.paw.kafkakeygenerator.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.kafkakeygenerator.exception.KafkaKeysNotFoundException
import no.nav.paw.kafkakeygenerator.exception.KafkaKeysUkjentFeilException
import no.nav.paw.kafkakeygenerator.model.HentAliasRequest
import no.nav.paw.kafkakeygenerator.model.HentAliasResponse
import no.nav.paw.kafkakeygenerator.model.HentKeysRequest
import no.nav.paw.kafkakeygenerator.model.HentKeysResponse

class DefaultKafkaKeyGeneratorClient(
    private val httpClient: HttpClient,
    private val url: String,
    private val getAccessToken: () -> String
) : KafkaKeyGeneratorClient {

    override suspend fun hent(ident: String): HentKeysResponse? {
        return httpClient.post("$url/api/v2/hent") {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(HentKeysRequest(ident))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> response.body<HentKeysResponse>()
                HttpStatusCode.NotFound -> null
                else -> throw KafkaKeysUkjentFeilException(response.status, response.body<String>())
            }
        }
    }

    override suspend fun hentEllerOpprett(ident: String): HentKeysResponse {
        return httpClient.post("$url/api/v2/hentEllerOpprett") {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(HentKeysRequest(ident))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> response.body<HentKeysResponse>()
                HttpStatusCode.NotFound -> throw KafkaKeysNotFoundException(response.body<String>())
                else -> throw KafkaKeysUkjentFeilException(response.status, response.body<String>())
            }
        }
    }

    override suspend fun hentAlias(antallPartisjoner: Int, identer: List<String>): HentAliasResponse {
        return httpClient.post("$url/api/v2/lokalInfo") {
            bearerAuth(getAccessToken())
            contentType(ContentType.Application.Json)
            setBody(HentAliasRequest(antallPartisjoner, identer))
        }.let { response ->
            when (response.status) {
                HttpStatusCode.OK -> response.body<HentAliasResponse>()
                HttpStatusCode.NotFound -> throw KafkaKeysNotFoundException(response.body<String>())
                else -> throw KafkaKeysUkjentFeilException(response.status, response.body<String>())
            }
        }
    }
}