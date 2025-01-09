package no.nav.paw.bekreftelse.api.policy

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.buildAuditLogger
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.error.model.map
import no.nav.paw.model.NavIdent
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.paw.tilgangskontroll.client.Tilgang
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun Action.asTilgang(): Tilgang = when (this) {
    Action.READ -> Tilgang.LESE
    Action.WRITE -> Tilgang.SKRIVE
}

class TilgangskontrollAccessPolicy(
    private val serverConfig: ServerConfig,
    private val tilgangskontrollClient: TilgangsTjenesteForAnsatte,
    private val identitetsnummer: Identitetsnummer?
) : AccessPolicy {

    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")
    private val auditLogger: Logger = buildAuditLogger

    override suspend fun hasAccess(action: Action, securityContext: SecurityContext): AccessDecision {
        return when (val bruker = securityContext.bruker) {
            is Sluttbruker -> {
                logger.debug("Ingen tilgangssjekk for sluttbruker")
                Permit("Sluttbruker har $action-tilgang")
            }

            is NavAnsatt -> {
                if (identitetsnummer == null) {
                    Deny("Veileder må sende med identitetsnummer for sluttbruker")
                } else {
                    val tilgang = action.asTilgang()
                    tilgangskontrollClient.harAnsattTilgangTilPerson(
                        navIdent = NavIdent(bruker.ident),
                        identitetsnummer = no.nav.paw.model.Identitetsnummer(identitetsnummer.verdi),
                        tilgang = tilgang
                    ).map { harTilgang ->
                        if (harTilgang) {
                            logger.debug("NAV-ansatt har benyttet {}-tilgang til informasjon om sluttbruker", tilgang)
                            auditLogger.audit(
                                runtimeEnvironment = serverConfig.runtimeEnvironment,
                                aktorIdent = bruker.ident,
                                sluttbrukerIdent = identitetsnummer.verdi,
                                action = action,
                                melding = "NAV-ansatt har benyttet $tilgang-tilgang til informasjon om sluttbruker"
                            )
                            Permit("Veileder har $tilgang-tilgang til sluttbruker")
                        }
                        else {
                            Deny("NAV-ansatt har ikke $tilgang-tilgang til sluttbruker")
                        }
                    }.getOrThrow()
                }
            }

            is Anonym -> {
                if (identitetsnummer != null) {
                    return Permit("M2M-token har $action-tilgang til sluttbruker")
                } else {
                    Deny("M2M-token må sende med identitetsnummer for sluttbruker")
                }
            }
        }
    }
}
