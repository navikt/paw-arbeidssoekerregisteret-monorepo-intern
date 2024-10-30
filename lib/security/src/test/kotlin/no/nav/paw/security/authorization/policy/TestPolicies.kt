package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.AccessResult

class TestPermitPolicy : AccessPolicy {

    override fun hasAccess(context: AuthorizationContext): AccessResult {
        return AccessResult(AccessDecision.PERMIT, "FULL STEAM AHEAD!")
    }
}

class TestDenyPolicy : AccessPolicy {

    override fun hasAccess(context: AuthorizationContext): AccessResult {
        return AccessResult(AccessDecision.DENY, "YOU SHALL NOT PASS!")
    }
}