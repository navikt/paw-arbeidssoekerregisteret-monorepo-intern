package no.nav.paw.bekreftelse.api.services

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.policy.TilgangskontrollAccessPolicy
import no.nav.paw.bekreftelse.api.policy.SluttbrukerAccessPolicy
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte

class AuthorizationService(
    private val serverConfig: ServerConfig,
    private val tilgangskontrollClient: TilgangsTjenesteForAnsatte
) {
    fun accessPolicies(identitetsnummer: Identitetsnummer? = null): List<AccessPolicy> {
        return listOf(
            SluttbrukerAccessPolicy(identitetsnummer),
            TilgangskontrollAccessPolicy(serverConfig, tilgangskontrollClient, identitetsnummer)
        )
    }
}

