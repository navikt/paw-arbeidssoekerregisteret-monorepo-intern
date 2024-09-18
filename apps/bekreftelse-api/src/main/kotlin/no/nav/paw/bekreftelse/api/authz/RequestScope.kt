package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.exception.BearerTokenManglerException
import no.nav.paw.bekreftelse.api.exception.UfullstendigBearerTokenException
import no.nav.paw.bekreftelse.api.model.BrukerType
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val sluttbruker: Sluttbruker,
    val innloggetBruker: InnloggetBruker,
    val claims: ResolvedClaims,
    val path: String,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
    val useMockData: Boolean
)

@Suppress("ConstPropertyName")
object NavHttpHeaders {
    const val TraceParent = "traceparent"
    const val NavCallId = "Nav-Call-Id"
    const val NavConsumerId = "Nav-Consumer-Id"
}

@WithSpan
suspend fun PipelineContext<Unit, ApplicationCall>.requestScope(
    identitetsnummer: String?,
    kafkaKeyClient: KafkaKeysClient,
    autorisasjonService: AutorisasjonService, // TODO Legg til autorisasjon
    tilgangType: TilgangType
): RequestScope {
    val bearerToken = call.request.headers[HttpHeaders.Authorization]
        ?: throw BearerTokenManglerException("Request mangler Bearer Token")

    val tokenValidationContext = call.principal<TokenValidationContextPrincipal>()

    val resolvedClaims = tokenValidationContext
        ?.context
        ?.resolveClaims(
            AzureName,
            AzureNavIdent,
            AzureOID,
            TokenXPID
        ) ?: ResolvedClaims()

    if (resolvedClaims.isEmpty()) {
        throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd innhold")
    }

    // TODO Håndtere at fnr kommer i body
    val resolvedIdentitetsnummer = if (resolvedClaims.isTokenX()) {
        resolvedClaims[TokenXPID]?.verdi
            ?: throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd innhold")
    } else if (resolvedClaims.isAzure()) {
        identitetsnummer ?: throw BadRequestException("Request mangler identitetsnummer")
    } else {
        throw UfullstendigBearerTokenException("Bearer Token er utstedt av ukjent issuer")
    }

    val kafkaKeysResponse = kafkaKeyClient.getIdAndKey(resolvedIdentitetsnummer)
    val sluttbruker = Sluttbruker(
        identitetsnummer = resolvedIdentitetsnummer,
        arbeidssoekerId = kafkaKeysResponse.id,
        kafkaKey = kafkaKeysResponse.key
    )

    val innloggetBruker = if (resolvedClaims.isTokenX()) {
        InnloggetBruker(
            type = BrukerType.SLUTTBRUKER,
            ident = resolvedIdentitetsnummer,
            bearerToken = bearerToken
        )
    } else {
        val ident = resolvedClaims[AzureNavIdent]
            ?: throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd innhold")
        InnloggetBruker(
            type = BrukerType.VEILEDER,
            ident = ident,
            bearerToken = bearerToken
        )
    }

    return RequestScope(
        sluttbruker = sluttbruker,
        innloggetBruker = innloggetBruker,
        claims = resolvedClaims,
        path = call.request.path(),
        callId = call.request.headers[NavHttpHeaders.NavCallId],
        traceparent = call.request.headers[NavHttpHeaders.TraceParent],
        navConsumerId = call.request.headers[NavHttpHeaders.NavConsumerId],
        useMockData = call.request.queryParameters["useMockData"].toBoolean()
    )
}
