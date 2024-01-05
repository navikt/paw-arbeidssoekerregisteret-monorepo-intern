package no.nav.paw.arbeidssokerregisteret

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssokerregisteret.utils.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val claims: ResolvedClaims,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
)

context(PipelineContext<Unit, ApplicationCall>)
fun requestScope(): RequestScope {
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
        claims = resolvedClaims,
        callId = headers["Nav-Call-Id"],
        traceparent = headers["traceparent"],
        navConsumerId = headers["Nav-Consumer-Id"]
    )
}

