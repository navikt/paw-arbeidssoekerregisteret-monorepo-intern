package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.utils.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val path: String,
    val claims: ResolvedClaims,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
)

@WithSpan
fun PipelineContext<Unit, ApplicationCall>.requestScope(): RequestScope {
    val tokenValidationContext = call.principal<TokenValidationContextPrincipal>()
    val resolvedClaims = tokenValidationContext
        ?.context
        ?.resolveClaims(
            AzureName,
            AzureNavIdent,
            AzureOID,
            TokenXPID
        ) ?: ResolvedClaims()
    val headers = call.request.headers
    return RequestScope(
        path = call.request.path(),
        claims = resolvedClaims,
        callId = headers["Nav-Call-Id"],
        traceparent = headers["traceparent"],
        navConsumerId = headers["Nav-Consumer-Id"]
    )
}
