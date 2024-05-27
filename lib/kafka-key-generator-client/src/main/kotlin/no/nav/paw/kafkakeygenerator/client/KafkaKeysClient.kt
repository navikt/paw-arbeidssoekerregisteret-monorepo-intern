package no.nav.paw.kafkakeygenerator.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.lang.IllegalStateException

data class KafkaKeysResponse(
    val id: Long,
    val key: Long
)

data class KafkaKeysRequest(
    val ident: String
)

interface KafkaKeysClient {
    suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse?
    suspend fun getIdAndKey(identitetsnummer: String): KafkaKeysResponse =
        getIdAndKeyOrNull(identitetsnummer) ?: throw IllegalStateException("Kafka-key-client: Uventet feil mot server: http-status=404")
}

class StandardKafkaKeysClient(
    private val httpClient: HttpClient,
    private val kafkaKeysUrl: String,
    private val getAccessToken: () -> String
) : KafkaKeysClient {
    override suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse? =
        httpClient.post(kafkaKeysUrl) {
            header("Authorization", "Bearer ${getAccessToken()}")
            contentType(ContentType.Application.Json)
            setBody(KafkaKeysRequest(identitetsnummer))
        }.let { response ->
            when (response.status) {
                io.ktor.http.HttpStatusCode.OK -> {
                    response.body<KafkaKeysResponse>()
                }

                io.ktor.http.HttpStatusCode.NotFound -> null
                else -> {
                    throw Exception("Kunne ikke hente kafka key, http_status=${response.status}, melding=${response.body<String>()}")
                }
            }
        }
}
