package no.nav.paw.bekreftelse.api.policy

import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.error.model.map
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.logging.logger.AuditLogger
import no.nav.paw.logging.logger.buildAuditLogger
import no.nav.paw.security.authentication.model.Anonym
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
import org.slf4j.LoggerFactory

private fun Action.asTilgang(): Tilgang = when (this) {
    Action.READ -> Tilgang.LESE
    Action.WRITE -> Tilgang.SKRIVE
}

private fun Action.asCefMessageEvent(): CefMessageEvent = when (this) {
    Action.READ -> CefMessageEvent.ACCESS
    Action.WRITE -> CefMessageEvent.UPDATE
}

class TilgangskontrollAccessPolicy(
    private val serverConfig: ServerConfig,
    private val tilgangskontrollClient: TilgangsTjenesteForAnsatte,
    private val identitetsnummer: Identitetsnummer?
) : AccessPolicy {

    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")
    private val auditLogger: AuditLogger = buildAuditLogger

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

            is NavAnsatt -> {
                if (identitetsnummer == null) {
                    Deny("Veileder må sende med identitetsnummer for sluttbruker")
                } else {
                    val tilgang = action.asTilgang()
                    tilgangskontrollClient.harAnsattTilgangTilPerson(
                        navIdent = NavIdent(bruker.ident),
                        identitetsnummer = identitetsnummer,
                        tilgang = tilgang
                    ).map { harTilgang ->
                        if (harTilgang) {
                            logger.debug("NAV-ansatt har benyttet {}-tilgang til informasjon om sluttbruker", tilgang)
                            auditLogger.audit(
                                runtimeEnvironment = serverConfig.runtimeEnvironment,
                                aktorIdent = bruker.ident,
                                sluttbrukerIdent = identitetsnummer.value,
                                event = action.asCefMessageEvent(),
                                melding = "NAV-ansatt har benyttet $tilgang-tilgang til informasjon om sluttbruker"
                            )
                            Permit("Veileder har $tilgang-tilgang til sluttbruker")
                        } else {
                            Deny("NAV-ansatt har ikke $tilgang-tilgang til sluttbruker")
                        }
                    }.getOrThrow()
                }
            }

            is Anonym -> {
                if (identitetsnummer == null) {
                    Deny("M2M-token må sende med identitetsnummer for sluttbruker")
                } else {
                    Permit("M2M-token har $action-tilgang til sluttbruker")
                }
            }
        }
    }
}
