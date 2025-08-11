package no.nav.paw.security.texas

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class TexasClient(
    private val config: TexasClientConfig,
    private val httpClient: HttpClient,
) {
    suspend fun getOnBehalfOfToken(userToken: String): OnBehalfOfResponse {
        val response = httpClient.post(config.endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                OnBehalfOfRequest(
                    userToken = userToken,
                    identityProvider = config.identityProvider,
                    target = config.target
                )
            )
        }

        if (response.status != HttpStatusCode.OK) {
            throw TokenExchangeException("Klarte ikke å veksle token. Statuskode: ${response.status.value}")
        }
        return response.body<OnBehalfOfResponse>()
    }
}

class TokenExchangeException(message: String) : RuntimeException(message)

data class OnBehalfOfRequest(
    @field:JsonProperty("user_token")
    val userToken: String,
    @field:JsonProperty("identity_provider")
    val identityProvider: String,
    val target: String,
)

data class OnBehalfOfResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
)