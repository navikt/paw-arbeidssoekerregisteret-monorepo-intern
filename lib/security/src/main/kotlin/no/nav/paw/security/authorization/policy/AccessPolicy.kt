package no.nav.paw.security.authorization.policy

import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessResult

interface AccessPolicy {
    fun hasAccess(context: AuthorizationContext): AccessResult
}
