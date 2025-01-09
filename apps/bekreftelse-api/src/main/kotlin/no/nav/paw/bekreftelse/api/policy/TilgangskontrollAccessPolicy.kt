package no.nav.paw.bekreftelse.api.policy

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.buildAuditLogger
import no.nav.paw.error.model.getOrThrow
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
        val tilgangType = action.asTilgang()
        val (bruker, _) = securityContext

        when (bruker) {
            is Sluttbruker -> {
                logger.debug("Ingen tilgangssjekk for sluttbruker")
                return Permit("Sluttbruker har $tilgangType-tilgang")
            }

            is NavAnsatt -> {
                if (identitetsnummer == null) {
                    return Deny("Veileder må sende med identitetsnummer for sluttbruker")
                }

                val harTilgang = tilgangskontrollClient.harAnsattTilgangTilPerson(
                    NavIdent(bruker.ident),
                    no.nav.paw.model.Identitetsnummer(identitetsnummer.verdi),
                    tilgangType
                ).getOrThrow()

                return if (harTilgang) {
                    logger.debug("NAV-ansatt har benyttet {}-tilgang til informasjon om sluttbruker", tilgangType)
                    auditLogger.audit(
                        runtimeEnvironment = serverConfig.runtimeEnvironment,
                        aktorIdent = bruker.ident,
                        sluttbrukerIdent = identitetsnummer.verdi,
                        action = action,
                        melding = "NAV-ansatt har benyttet $tilgangType-tilgang til informasjon om sluttbruker"
                    )
                    Permit("Veileder har $tilgangType-tilgang til sluttbruker")
                } else {
                    Deny("NAV-ansatt har ikke $tilgangType-tilgang til sluttbruker")
                }
            }

            is Anonym -> {
                if (identitetsnummer != null) {
                    return Permit("M2M-token har $tilgangType-tilgang til sluttbruker")
                }
                return Deny("M2M-token må sende med identitetsnummer for sluttbruker")
            }

            else -> {
                return Deny("Ukjent brukergruppe")
            }
        }
    }
}