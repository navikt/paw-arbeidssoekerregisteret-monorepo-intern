package no.nav.paw.bekreftelse.api.service

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.policy.TilgangskontrollAccessPolicy
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte

class AuthorizationService(
    private val serverConfig: ServerConfig,
    private val tilgangskontrollClient: TilgangsTjenesteForAnsatte
) {
    fun accessPolicies(identitetsnummer: Identitetsnummer? = null): List<AccessPolicy> {
        return listOf(
            TilgangskontrollAccessPolicy(serverConfig, tilgangskontrollClient, identitetsnummer)
        )
    }
}

