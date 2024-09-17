package no.nav.paw.bekreftelse.api.authz

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.model.Identitetsnummer
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val identitetsnummer: Identitetsnummer,
    val arbeidssoekerId: Long,
    val claims: ResolvedClaims,
    val path: String,
    val bearerToken: String,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
)

@WithSpan
suspend fun PipelineContext<Unit, ApplicationCall>.requestScope(
    identitetsnummer: String,
    kafkaKeyClient: KafkaKeysClient,
    autorisasjonService: AutorisasjonService, // TODO Legg til autorisasjon
    tilgangType: TilgangType
): RequestScope {
    val tokenValidationContext = call.principal<TokenValidationContextPrincipal>()

    val resolvedClaims = tokenValidationContext
        ?.context
        ?.resolveClaims(
            AzureName,
            AzureNavIdent,
            AzureOID,
            TokenXPID
        ) ?: ResolvedClaims()

    val kafkaKeysResponse = kafkaKeyClient.getIdAndKey(identitetsnummer)
    val headers = call.request.headers

    return RequestScope(
        identitetsnummer = Identitetsnummer(identitetsnummer),
        arbeidssoekerId = kafkaKeysResponse.id,
        claims = resolvedClaims,
        path = call.request.path(),
        bearerToken = headers["Authorization"] ?: throw BearerTokenManglerException("Request mangler Bearer Token"),
        callId = headers["Nav-Call-Id"],
        traceparent = headers["traceparent"],
        navConsumerId = headers["Nav-Consumer-Id"]
    )
}
