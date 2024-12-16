package no.nav.paw.security.authorization.interceptor

import io.ktor.server.routing.RoutingContext
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.context.resolveRequestContext
import no.nav.paw.security.authorization.context.resolveSecurityContext
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")

suspend fun RoutingContext.authorize(
    action: Action,
    accessPolicies: List<AccessPolicy> = emptyList(),
    body: suspend RoutingContext.(AuthorizationContext) -> Unit
): RoutingContext {
    logger.debug("Kjører autorisasjon")
    val requestContext = resolveRequestContext()
    val securityContext = requestContext.resolveSecurityContext()
    val authorizationContext = AuthorizationContext(requestContext, securityContext)
    accessPolicies.forEach { it.checkAccess(action, authorizationContext) }
    body(authorizationContext)
    return this
}