package no.nav.paw.bekreftelse.api.context

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

sealed class NavHeader(val name: String)

data object TraceParent : NavHeader("traceparent")
data object NavCallId : NavHeader("Nav-Call-Id")
data object NavConsumerId : NavHeader("Nav-Consumer-Id")

data class RequestContext(
    val path: String,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
    val bearerToken: String?,
    val identitetsnummer: String?,
    val useMockData: Boolean,
    val principal: TokenValidationContextPrincipal?
)

fun PipelineContext<Unit, ApplicationCall>.resolveRequest(
    identitetsnummer: String? = null
): RequestContext {
    return RequestContext(
        path = call.request.path(),
        callId = call.request.headers[NavCallId.name],
        traceparent = call.request.headers[TraceParent.name],
        navConsumerId = call.request.headers[NavConsumerId.name],
        bearerToken = call.request.headers[HttpHeaders.Authorization],
        identitetsnummer = identitetsnummer,
        useMockData = call.request.queryParameters["useMockData"].toBoolean(),
        principal = call.principal<TokenValidationContextPrincipal>()
    )
}