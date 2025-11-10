package no.nav.paw.tilgangskontroll.poaotilgang

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.tilgangskontroll.SecureLogger
import no.nav.paw.tilgangskontroll.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.poaotilgang.api.DecisionType
import no.nav.paw.tilgangskontroll.poaotilgang.api.EvaluatePoliciesResponse
import no.nav.paw.tilgangskontroll.poaotilgang.api.navAnsattTilgangTilEksternBrukerPolicyInputV1Dto
import no.nav.paw.tilgangskontroll.poaotilgang.api.toEvaluatePoliciesRequest
import no.nav.paw.tilgangskontroll.vo.Tilgang
import java.net.URI

private const val V1_POLICY_EVEAL_PATH = "/api/v1/policy/evaluate"

class PoaoTilgangsTjeneste(
    private val secureLogger: SecureLogger,
    private val httpClient: HttpClient,
    poaTilgangUrl: URI,
    private val poaoToken: () -> String
) : TilgangsTjenesteForAnsatte {
    private val v1PolicyEvalUri = poaTilgangUrl.resolve(V1_POLICY_EVEAL_PATH)

    override suspend fun harAnsattTilgangTilPerson(
        navIdent: NavIdent,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Boolean =
        httpClient.post {
            url { takeFrom(v1PolicyEvalUri) }
            contentType(ContentType.Application.Json)
            bearerAuth(poaoToken())
            setBody(
                navAnsattTilgangTilEksternBrukerPolicyInputV1Dto(
                    navIdent = navIdent,
                    identitetsnummer = identitetsnummer,
                    tilgang = tilgang
                ).toEvaluatePoliciesRequest()
            )
        }.let { httpResponse ->
            if (httpResponse.status.isSuccess()) {
                httpResponse.body<EvaluatePoliciesResponse>()
            } else {
                val body = kotlin.runCatching { httpResponse.body<String>() }.getOrElse { "" }
                secureLogger.error(
                    "Feil mot poao-tilgang: url='{}', status='{} {}', body='{}'",
                    v1PolicyEvalUri,
                    httpResponse.status.value,
                    httpResponse.status.description,
                    body
                )
                throw ServerResponseException(
                    status = HttpStatusCode.BadRequest,
                    type = ErrorType.domain("tilgangskontroll").error("feil-ved-evaluering-av-tilgangspolicy").build(),
                    message = "Feil ved evaluering av tilgangspolicy"
                )
            }
        }.let { responseDto ->
            responseDto.results.all { it.decision.type == DecisionType.PERMIT }
        }
}
