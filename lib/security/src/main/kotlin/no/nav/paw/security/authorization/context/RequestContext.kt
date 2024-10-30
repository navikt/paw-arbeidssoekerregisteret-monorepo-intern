package no.nav.paw.security.authorization.context

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

sealed class NavHeader(val name: String)

data object TraceParent : NavHeader("traceparent")
data object NavCallId : NavHeader("Nav-Call-Id")
data object NavConsumerId : NavHeader("Nav-Consumer-Id")
data object NavIdent : NavHeader("Nav-Ident")

data class RequestHeaders(
    val authorization: String?,
    val navCallId: String?,
    val traceParent: String?,
    val navConsumerId: String?,
    val navIdent: String?,
)

data class RequestContext(
    val request: ApplicationRequest,
    val headers: RequestHeaders,
    val principal: TokenValidationContextPrincipal?
)

fun PipelineContext<Unit, ApplicationCall>.resolveRequestContext(): RequestContext {
    return RequestContext(
        request = call.request,
        headers = resolveRequestHeaders(),
        principal = call.principal<TokenValidationContextPrincipal>()
    )
}

fun PipelineContext<Unit, ApplicationCall>.resolveRequestHeaders(): RequestHeaders {
    return RequestHeaders(
        authorization = call.request.headers[HttpHeaders.Authorization],
        traceParent = call.request.headers[TraceParent.name],
        navCallId = call.request.headers[NavCallId.name],
        navConsumerId = call.request.headers[NavConsumerId.name],
        navIdent = call.request.headers[NavIdent.name],
    )
}