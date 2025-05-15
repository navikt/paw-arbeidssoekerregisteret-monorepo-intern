package no.nav.paw.client.api.oppslag.factory

import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.client.api.oppslag.config.API_OPPSLAG_CLIENT_CONFIG
import no.nav.paw.client.api.oppslag.config.ApiOppslagClientConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

object ApiOppslagClientFactory {
    fun buildApiOppslagClient(
        clientConfig: ApiOppslagClientConfig = loadClientConfig(),
        tokenClient: AzureAdMachineToMachineTokenClient = createAzureAdM2MTokenClient(),
    ): ApiOppslagClient {
        return ApiOppslagClient(
            baseUrl = clientConfig.url
        ) {
            tokenClient.createMachineToMachineToken(clientConfig.scope)
        }
    }

    fun loadClientConfig(resource: String = API_OPPSLAG_CLIENT_CONFIG): ApiOppslagClientConfig {
        return loadNaisOrLocalConfiguration<ApiOppslagClientConfig>(resource)
    }
}