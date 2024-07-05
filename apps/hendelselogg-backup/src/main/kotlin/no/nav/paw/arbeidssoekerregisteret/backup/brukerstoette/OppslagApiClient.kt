package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.*
import java.util.*

class OppslagApiClient(
    config: OppslagApiConfig,
    private val getAccessToken: () -> String,
    private val httpClient: HttpClient
) {
    private val perioderUrl = "${config.baseUrl}${config.perioderPath}"
    private val opplysningerUrl = "${config.baseUrl}${config.opplysningerPath}"
    private val profileringUrl = "${config.baseUrl}${config.profileringPath}"

    suspend fun perioder(identitetsnunmer: String): Either<Error, List<ArbeidssoekerperiodeResponse>> {
        val response = httpClient.post(perioderUrl) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append("Authorization", "Bearer ${getAccessToken()}")
            }
            setBody(ArbeidssoekerperiodeRequest(identitetsnunmer))
        }
        return when (response.status.value) {
            HttpStatusCode.OK.value -> response.body<List<ArbeidssoekerperiodeResponse>>().right()
            HttpStatusCode.NotFound.value -> emptyList<ArbeidssoekerperiodeResponse>().right()
            else -> Error(response.status.description, response.status.value).left()
        }
    }

    suspend fun opplysninger(
        identitetsnunmer: String,
        periodeId: UUID
    ): Either<Error, List<OpplysningerOmArbeidssoekerResponse>> {
        val response = httpClient.post(opplysningerUrl) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append("Authorization", "Bearer ${getAccessToken()}")
            }
            setBody(OpplysningerOmArbeidssoekerRequest(
                identitetsnummer = identitetsnunmer,
                periodeId = periodeId
            ))
        }
        return when (response.status.value) {
            HttpStatusCode.OK.value -> response.body<List<OpplysningerOmArbeidssoekerResponse>>().right()
            HttpStatusCode.NotFound.value -> emptyList<OpplysningerOmArbeidssoekerResponse>().right()
            else -> Error(response.status.description, response.status.value).left()
        }
    }

    suspend fun profileringer(
        identitetsnunmer: String,
        periodeId: UUID
    ): Either<Error, List<ProfileringResponse>> {
        val response = httpClient.post(profileringUrl) {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                append("Authorization", "Bearer ${getAccessToken()}")
            }
            setBody(ProfileringRequest(
                identitetsnummer = identitetsnunmer,
                periodeId = periodeId
            ))
        }
        return when (response.status.value) {
            HttpStatusCode.OK.value -> response.body<List<ProfileringResponse>>().right()
            HttpStatusCode.NotFound.value -> emptyList<ProfileringResponse>().right()
            else -> Error(response.status.description, response.status.value).left()
        }
    }
}

const val OPPSLAG_API_CONFIG = "api_oppslag_configuration.toml"

data class OppslagApiConfig(
    val baseUrl: String,
    val scope: String,
    val perioderPath: String,
    val opplysningerPath: String,
    val profileringPath: String
)

data class Error(
    val message: String,
    val httpCode: Int
)