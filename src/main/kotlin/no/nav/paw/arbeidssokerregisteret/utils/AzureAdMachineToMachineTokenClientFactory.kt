package no.nav.paw.arbeidssokerregisteret.utils

import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.arbeidssokerregisteret.config.AuthProvider
import no.nav.paw.arbeidssokerregisteret.config.NaisEnv

fun azureAdM2MTokenClient(naisEnv: NaisEnv, azureProviderConfig: AuthProvider): AzureAdMachineToMachineTokenClient =
    when (naisEnv) {
        NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(azureProviderConfig.clientId)
            .withPrivateJwk(createMockRSAKey("azure"))
            .withTokenEndpointUrl(azureProviderConfig.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient()
    }
