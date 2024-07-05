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
    private val url: String,
    private val getAccessToken: () -> String,
    private val httpClient: HttpClient
) {
    private val perioderPath = "/api/v1/veileder/arbeidssoekerperioder"
    private val opplysningerPath = "/api/v1/veileder/opplysninger-om-arbeidssoeker"
    private val profileringPath = "/api/v1/veileder/profilering"

    suspend fun perioder(identitetsnunmer: String): Either<Error, List<ArbeidssoekerperiodeResponse>> {
        val response = httpClient.post(url + perioderPath) {
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
        val response = httpClient.post(url + opplysningerPath) {
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
        val response = httpClient.post(url + profileringPath) {
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

const val OPPSLAG_API_CONFIG = "oppslags_api_config.toml"

data class OppslagApiConfig(
    val url: String,
    val scope: String
)

data class Error(
    val message: String,
    val httpCode: Int
)