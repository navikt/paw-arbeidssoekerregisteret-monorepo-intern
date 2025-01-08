package no.nav.paw.tilgangskontroll.poaotilgang

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import no.nav.paw.tilgangskontroll.RemoteHttpException
import no.nav.paw.tilgangskontroll.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.poaotilgang.api.DecisionType
import no.nav.paw.tilgangskontroll.poaotilgang.api.EvaluatePoliciesResponse
import no.nav.paw.tilgangskontroll.poaotilgang.api.navAnsattTilgangTilEksternBrukerPolicyInputV1Dto
import no.nav.paw.tilgangskontroll.poaotilgang.api.toEvaluatePoliciesRequest
import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.NavIdent
import no.nav.paw.tilgangskontroll.vo.Tilgang
import java.net.URI

private const val V1_POLICY_EVEAL_PATH = "/api/v1/policy/evaluate"

class PoaoTilgangsTjeneste(
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
            headers {
                append("Authorization", "Bearer ${poaoToken()}")
            }
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
                throw RemoteHttpException(
                    msg = "Ekstern feil ved evaluering av policy",
                    statusCode = httpResponse.status.value,
                    statusMessage = httpResponse.status.description
                )
            }
        }.let { responseDto ->
            responseDto.results.all { it.decision.type == DecisionType.PERMIT }
        }
}
