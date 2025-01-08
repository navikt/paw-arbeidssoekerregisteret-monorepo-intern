package no.nav.paw.security.authorization.interceptor

import io.ktor.server.routing.RoutingContext
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.policy.AccessPolicy
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")

suspend fun RoutingContext.autorisering(
    action: Action,
    accessPolicies: List<AccessPolicy> = emptyList(),
    body: suspend RoutingContext.() -> Unit
): RoutingContext {
    logger.debug("Kjører autorisasjon")
    val securityContext = call.securityContext()
    accessPolicies.forEach { it.checkAccess(action, securityContext) }
    body()
    return this
}
