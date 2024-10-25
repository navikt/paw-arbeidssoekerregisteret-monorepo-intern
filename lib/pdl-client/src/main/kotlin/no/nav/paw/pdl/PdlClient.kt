package no.nav.paw.pdl

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI

// Se https://pdldocs-navno.msappproxy.net/ for dokumentasjon av PDL API-et
class PdlClient(
    url: String,
    // Tema: https://confluence.adeo.no/pages/viewpage.action?pageId=309311397
    private val tema: String,
    httpClient: HttpClient,
    private val getAccessToken: () -> String,
) {
    internal val logger = LoggerFactory.getLogger(this::class.java)

    private val graphQLClient =
        GraphQLKtorClient(
            url = URI.create(url).toURL(),
            httpClient = httpClient,
        )

    internal suspend fun <T : Any> execute(
        query: GraphQLClientRequest<T>,
        behandlingsnummer: String,
        callId: String?,
        traceparent: String? = null,
        navConsumerId: String?,
    ): GraphQLClientResponse<T> {
        if (behandlingsnummer.isBlank()) {
            throw IllegalArgumentException("Behandlingsnummer kan ikke være tom")
        }
        return graphQLClient.execute(query) {
            bearerAuth(getAccessToken())
            header("Tema", tema)
            header("Nav-Call-Id", callId)
            header("Nav-Consumer-Id", navConsumerId)
            header("Behandlingsnummer", behandlingsnummer)
            traceparent?.let { header("traceparent", it) }
        }
    }
}

class PdlException(
    message: String? = null,
    val errors: List<GraphQLClientError>?,
) : IOException(message)
