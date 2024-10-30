package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authentication.exception.IngenTilgangException
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import org.slf4j.LoggerFactory

class AccessPolicyEvaluator(private val policies: List<AccessPolicy>) {

    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")

    fun checkAccess(context: AuthorizationContext) {
        logger.debug("Sjekker {} tilgangsregler", policies.size)
        val results = policies.map { it.hasAccess(context) }
        val deny = results.filter { it.decision == AccessDecision.DENY }
        if (deny.isNotEmpty()) {
            logger.debug("Evaluering av tilgangsregler resulterte i {} DENY decisions [{}]", deny.size, deny)
            throw IngenTilgangException("Ingen tilgang")
        }
    }
}