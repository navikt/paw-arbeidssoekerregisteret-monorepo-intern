package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.context.NavCallId
import no.nav.paw.bekreftelse.api.context.NavConsumerId
import no.nav.paw.bekreftelse.api.context.RequestContext
import no.nav.paw.bekreftelse.api.context.TraceParent
import no.nav.paw.bekreftelse.api.exception.BearerTokenManglerException
import no.nav.paw.bekreftelse.api.exception.UfullstendigBearerTokenException
import no.nav.paw.bekreftelse.api.exception.UkjentBearerTokenException
import no.nav.paw.bekreftelse.api.services.AuthorizationService
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

@WithSpan
suspend fun PipelineContext<Unit, ApplicationCall>.authorize(
    identitetsnummer: String?,
    authorizationService: AuthorizationService,
    tilgangType: TilgangType
): RequestContext {
    val bearerToken = call.request.headers[HttpHeaders.Authorization]
        ?: throw BearerTokenManglerException("Request mangler Bearer Token")

    val principal = call.principal<TokenValidationContextPrincipal>()
    val accessToken = principal
        ?.context
        ?.resolveTokens()
        ?: throw UkjentBearerTokenException("Fant ikke token med forventet issuer")

    if (accessToken.claims.isEmpty()) {
        throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd innhold")
    }

    if (!accessToken.isValidIssuer()) {
        throw UkjentBearerTokenException("Bearer Token er utstedt av ukjent issuer")
    }

    val sluttbruker = authorizationService.resolveSluttbruker(accessToken, identitetsnummer)
    val innloggetBruker = authorizationService.resolveInnloggetBruker(bearerToken, accessToken)

    //authorizationService.authorize(accessToken, sluttbruker, tilgangType) // TODO Legg til autorisasjon (POAO Tilgang)

    return RequestContext(
        path = call.request.path(),
        callId = call.request.headers[NavCallId.name],
        traceparent = call.request.headers[TraceParent.name],
        navConsumerId = call.request.headers[NavConsumerId.name],
        useMockData = call.request.queryParameters["useMockData"].toBoolean(),
        sluttbruker = sluttbruker,
        innloggetBruker = innloggetBruker,
        accessToken = accessToken,
    )
}
