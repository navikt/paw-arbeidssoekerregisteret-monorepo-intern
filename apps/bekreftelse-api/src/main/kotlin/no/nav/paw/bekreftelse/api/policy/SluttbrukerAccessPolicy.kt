package no.nav.paw.bekreftelse.api.policy

import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy

class SluttbrukerAccessPolicy(
    private val identitetsnummer: Identitetsnummer?
) : AccessPolicy {

    override fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision {
        val (bruker, _) = context.securityContext

        when (bruker) {
            is Sluttbruker -> {
                // TODO Håndtere verge
                if (identitetsnummer != null && identitetsnummer != bruker.ident) {
                    return Deny("Sluttbruker har ikke tilgang til data for annen bruker")
                }
                return Permit("Sluttbruker har tilgang")
            }

            else -> {
                return Permit("Ikke sluttbruker")
            }
        }
    }
}