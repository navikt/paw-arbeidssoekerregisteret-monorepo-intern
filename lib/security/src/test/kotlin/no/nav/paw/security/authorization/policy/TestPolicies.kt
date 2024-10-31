package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit

class TestPermitPolicy : AccessPolicy {

    override fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision {
        return Permit("FULL STEAM AHEAD!")
    }
}

class TestDenyPolicy : AccessPolicy {

    override fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision {
        return Deny("YOU SHALL NOT PASS!")
    }
}