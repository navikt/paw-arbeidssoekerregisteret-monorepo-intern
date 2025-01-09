package no.nav.paw.tilgangskontroll.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.fullPath
import io.ktor.http.isSuccess
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.model.NavIdent
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.tilgangskontroll.server.models.TilgangskontrollRequestV1
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.nav.paw.tilgangskontroll.server.models.TilgangskontrollResponseV1
import java.util.*

interface TilgangsTjenesteForAnsatte {
    suspend fun harAnsattTilgangTilPerson(
        navIdent: NavIdent,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Response<Boolean>
}

fun tilgangsTjenesteForAnsatte(
    httpClient: HttpClient,
    config: TilgangskontrollClientConfig,
    tokenProvider: (String) -> String
): TilgangsTjenesteForAnsatte = TilgangsTjenesteForAnsatteImpl(httpClient, config, tokenProvider)

private class TilgangsTjenesteForAnsatteImpl(
    private val httpClient: HttpClient,
    config: TilgangskontrollClientConfig,
    tokenPrivder: (String) -> String
) : TilgangsTjenesteForAnsatte {
    private val apiTilgangV1 = config.apiTilgangV1()
    private val tokenProvider = { tokenPrivder(config.scope) }

    override suspend fun harAnsattTilgangTilPerson(
        navIdent: NavIdent,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Response<Boolean> {
        val response = httpClient.post(apiTilgangV1.toString()) {
            bearerAuth(tokenProvider())
            contentType(ContentType.Application.Json)
            setBody(TilgangskontrollRequestV1(
                identitetsnummer = identitetsnummer.verdi,
                navAnsattId = navIdent.verdi,
                tilgang = tilgang.toApi()
            ))
        }
        return mapResponse<TilgangskontrollResponseV1>(response)
            .map { it.harTilgang }
    }
}

suspend inline fun <reified T> mapResponse(response: HttpResponse): Response<T> {
    return when {
        response.status.isSuccess() -> Data(response.body<T>())
        else -> {
            runCatching {
                response.body<ProblemDetails>()
            }.getOrElse {
                ProblemDetails(
                    id = UUID.randomUUID(),
                    type = response.status.toErrorType().build(),
                    status = response.status,
                    title = response.status.description,
                    detail = response.status.description,
                    instance = response.request.url.fullPath
                )
            }
        }
    }
}

fun HttpStatusCode.toErrorType(): ErrorType {
    return ErrorType.domain("http").error(this.description.replace(" ", "_").lowercase())
}

fun Tilgang.toApi(): TilgangskontrollRequestV1.Tilgang {
    return when (this) {
        Tilgang.LESE -> TilgangskontrollRequestV1.Tilgang.LESE
        Tilgang.SKRIVE -> TilgangskontrollRequestV1.Tilgang.SKRIVE
        Tilgang.LESE_SKRIVE -> TilgangskontrollRequestV1.Tilgang.LESE_SKRIVE
    }
}