package no.nav.paw.security.authorization.interceptor

import io.ktor.server.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.context.resolveRequestContext
import no.nav.paw.security.authorization.context.resolveSecurityContext
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")

suspend fun PipelineContext<Unit, ApplicationCall>.authorize(
    action: Action,
    accessPolicies: List<AccessPolicy> = emptyList(),
    body: suspend PipelineContext<Unit, ApplicationCall>.(AuthorizationContext) -> Unit
): PipelineContext<Unit, ApplicationCall> {
    logger.debug("Kjører autorisasjon")
    val requestContext = resolveRequestContext()
    val securityContext = requestContext.resolveSecurityContext()
    val authorizationContext = AuthorizationContext(requestContext, securityContext)
    accessPolicies.forEach { it.checkAccess(action, authorizationContext) }
    body(authorizationContext)
    return this
}