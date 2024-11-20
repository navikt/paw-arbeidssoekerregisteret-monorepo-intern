package no.nav.paw.kafkakeygenerator.pdl

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.pdl.PdlClient

fun opprettPdlKlient(
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
