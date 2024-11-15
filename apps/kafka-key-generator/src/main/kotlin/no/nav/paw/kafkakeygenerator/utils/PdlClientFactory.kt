package no.nav.paw.kafkakeygenerator.utils

import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.pdl.PdlClient

fun createPdlClient(
    pdlClientConfig: PdlClientConfig,
    azureAdM2MConfig: AzureAdM2MConfig
): PdlClient {
    val azureTokenClient = createAzureAdM2MTokenClient(azureProviderConfig = azureAdM2MConfig)
    return PdlClient(
        url = pdlClientConfig.url,
        tema = pdlClientConfig.tema,
        httpClient = createHttpClient()
    ) { azureTokenClient.createMachineToMachineToken(pdlClientConfig.scope) }
}
