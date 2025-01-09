package no.nav.paw.bekreftelse.api.policy

import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy

class SluttbrukerAccessPolicy(
    private val identitetsnummer: Identitetsnummer?
) : AccessPolicy {

    override suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        return when (val bruker = securityContext.bruker) {
            is Sluttbruker -> {
                // TODO Håndtere verge
                if (identitetsnummer != null && identitetsnummer != bruker.ident) {
                    Deny("Sluttbruker har ikke tilgang til data for annen bruker")
                } else {
                    Permit("Sluttbruker har tilgang")
                }
            }

            else -> {
                Permit("Ikke sluttbruker")
            }
        }
    }
}