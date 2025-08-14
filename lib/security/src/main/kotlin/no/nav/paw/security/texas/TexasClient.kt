import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.paw.security.texas.OnBehalfOfAnsattRequest
import no.nav.paw.security.texas.OnBehalfOfBrukerRequest
import no.nav.paw.security.texas.OnBehalfOfRequest
import no.nav.paw.security.texas.TexasClientConfig

class TexasClient(
    private val config: TexasClientConfig,
    private val httpClient: HttpClient,
) {
    suspend fun exchangeOnBehalfOfBrukerToken(request: OnBehalfOfBrukerRequest) = exchangeToken(request)

    suspend fun exchangeOnBehalfOfAnsattToken(request: OnBehalfOfAnsattRequest) = exchangeToken(request)

    private suspend fun exchangeToken(onBehalfOfRequest: OnBehalfOfRequest): OnBehalfOfResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(ContentType.Application.Json)
            setBody(onBehalfOfRequest)
        }
        if (response.status != HttpStatusCode.OK) {
            throw TokenExchangeException("Klarte ikke å veksle token. Statuskode: ${response.status.value}")
        }
        return response.body<OnBehalfOfResponse>()
    }
}

class TokenExchangeException(message: String) : RuntimeException(message)

data class OnBehalfOfResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
)