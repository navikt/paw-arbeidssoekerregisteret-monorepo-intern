package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.exception.IngenTilgangException
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Decision

interface AccessPolicy {
    fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision

    fun checkAccess(action: Action, context: AuthorizationContext) {
        val accessDecision = hasAccess(action, context)
        if (accessDecision.decision == Decision.DENY) {
            throw IngenTilgangException(accessDecision.description)
        }
    }
}
