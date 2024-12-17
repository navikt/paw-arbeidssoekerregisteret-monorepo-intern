package no.nav.paw.security.authorization.context

import io.ktor.http.HttpHeaders
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.RoutingContext
import no.nav.security.token.support.v3.TokenValidationContextPrincipal

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

fun RoutingContext.resolveRequestContext(): RequestContext {
    return RequestContext(
        request = call.request,
        headers = resolveRequestHeaders(),
        principal = call.principal<TokenValidationContextPrincipal>()
    )
}

fun RoutingContext.resolveRequestHeaders(): RequestHeaders {
    return RequestHeaders(
        authorization = call.request.headers[HttpHeaders.Authorization],
        traceParent = call.request.headers[TraceParent.name],
        navCallId = call.request.headers[NavCallId.name],
        navConsumerId = call.request.headers[NavConsumerId.name],
        navIdent = call.request.headers[NavIdent.name],
    )
}