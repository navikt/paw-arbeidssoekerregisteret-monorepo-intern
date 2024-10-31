package no.nav.paw.bekreftelse.api.policy

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.buildAuditLogger
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.M2MToken
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.context.AuthorizationContext
import no.nav.paw.security.authorization.model.AccessDecision
import no.nav.paw.security.authorization.model.Action
import no.nav.paw.security.authorization.model.Deny
import no.nav.paw.security.authorization.model.Permit
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun Action.asTilgangType(): TilgangType = when (this) {
    Action.READ -> TilgangType.LESE
    Action.WRITE -> TilgangType.SKRIVE
}

class PoaoTilgangAccessPolicy(
    private val serverConfig: ServerConfig,
    private val poaoTilgangClient: PoaoTilgangClient,
    private val identitetsnummer: Identitetsnummer?
) : AccessPolicy {

    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authorization")
    private val auditLogger: Logger = buildAuditLogger

    override fun hasAccess(action: Action, context: AuthorizationContext): AccessDecision {
        val tilgangType = action.asTilgangType()
        val (bruker, _) = context.securityContext

        when (bruker) {
            is Sluttbruker -> {
                logger.debug("Ingen tilgangssjekk for sluttbruker")
                return Permit("Sluttbruker har $tilgangType-tilgang")
            }

            is NavAnsatt -> {
                if (identitetsnummer == null) {
                    return Deny("Veileder må sende med identitetsnummer for sluttbruker")
                }

                val navAnsattTilgang = poaoTilgangClient.evaluatePolicy(
                    NavAnsattTilgangTilEksternBrukerPolicyInput(
                        navAnsattAzureId = bruker.oid,
                        tilgangType = tilgangType,
                        norskIdent = identitetsnummer.verdi
                    )
                )
                val tilgang = navAnsattTilgang.get()
                if (tilgang == null) {
                    return Deny("Kunne ikke finne tilgang for ansatt")
                } else if (tilgang.isDeny) {
                    return Deny("NAV-ansatt har ikke $tilgangType-tilgang til sluttbruker")
                } else {
                    logger.debug("NAV-ansatt har benyttet {}-tilgang til informasjon om sluttbruker", tilgangType)
                    auditLogger.audit(
                        runtimeEnvironment = serverConfig.runtimeEnvironment,
                        aktorIdent = bruker.ident,
                        sluttbrukerIdent = identitetsnummer.verdi,
                        tilgangType = tilgangType,
                        melding = "NAV-ansatt har benyttet $tilgangType-tilgang til informasjon om sluttbruker"
                    )
                    return Permit("Veileder har $tilgangType-tilgang til sluttbruker")
                }
            }

            is M2MToken -> {
                if (identitetsnummer == null) {
                    return Deny("M2M-token må sende med identitetsnummer for sluttbruker")
                }
                return Permit("M2M-token har $tilgangType-tilgang til sluttbruker")
            }

            else -> {
                return Deny("Ukjent brukergruppe")
            }
        }
    }
}