package no.nav.paw.security.authentication.model

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest

sealed class NavHeader(val name: String)

data object TraceParentHeader : NavHeader("traceparent")
data object NavCallIdHeader : NavHeader("Nav-Call-Id")
data object NavConsumerIdHeader : NavHeader("Nav-Consumer-Id")
data object NavIdentHeader : NavHeader("Nav-Ident")

data class RequestHeaders(
    val authorization: String?,
    val navCallId: String?,
    val traceParent: String?,
    val navConsumerId: String?,
    val navIdent: String?,
)

data class RequestContext(
    val request: ApplicationRequest,
    val headers: RequestHeaders
)

fun ApplicationCall.resolveRequestContext(): RequestContext {
    return RequestContext(
        request = request,
        headers = resolveRequestHeaders()
    )
}

fun ApplicationCall.resolveRequestHeaders(): RequestHeaders {
    return RequestHeaders(
        authorization = request.headers[HttpHeaders.Authorization],
        traceParent = request.headers[TraceParentHeader.name],
        navCallId = request.headers[NavCallIdHeader.name],
        navConsumerId = request.headers[NavConsumerIdHeader.name],
        navIdent = request.headers[NavIdentHeader.name],
    )
}