package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Decision

interface AccessPolicy {
    suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision

    suspend fun checkAccess(action: Action, securityContext: SecurityContext) {
        val accessDecision = hasAccess(action, securityContext)
        if (accessDecision.decision == Decision.DENY) {
            throw IngenTilgangException(accessDecision.description)
        }
    }
}
