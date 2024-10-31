package no.nav.paw.bekreftelse.api.services

import no.nav.paw.bekreftelse.api.config.ServerConfig
import no.nav.paw.bekreftelse.api.policy.PoaoTilgangAccessPolicy
import no.nav.paw.bekreftelse.api.policy.SluttbrukerAccessPolicy
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authorization.policy.AccessPolicy
import no.nav.poao_tilgang.client.PoaoTilgangClient

class AuthorizationService(
    private val serverConfig: ServerConfig,
    private val poaoTilgangClient: PoaoTilgangClient
) {
    fun accessPolicies(identitetsnummer: Identitetsnummer? = null): List<AccessPolicy> {
        return listOf(
            SluttbrukerAccessPolicy(identitetsnummer),
            PoaoTilgangAccessPolicy(serverConfig, poaoTilgangClient, identitetsnummer)
        )
    }
}

