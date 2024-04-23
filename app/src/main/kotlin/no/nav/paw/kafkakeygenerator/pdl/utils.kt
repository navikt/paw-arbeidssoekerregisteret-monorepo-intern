package no.nav.paw.kafkakeygenerator.pdl

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.cache.CaffeineTokenCache
import no.nav.paw.kafkakeygenerator.config.AzureTokenKlientKonfigurasjon
import no.nav.paw.kafkakeygenerator.config.PdlKlientKonfigurasjon
import no.nav.paw.pdl.PdlClient

fun opprettKtorKlient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        jackson()
    }
}

fun opprettPdlKlient(
    konfig: PdlKlientKonfigurasjon,
    autentiseringskonfigurasjon: AzureTokenKlientKonfigurasjon
) = PdlClient(
    url = konfig.url,
    tema = konfig.tema,
    httpClient = opprettKtorKlient()
) { getAccessToken(konfig, autentiseringskonfigurasjon) }

private fun aadMachineToMachineTokenClient(konfig: AzureTokenKlientKonfigurasjon) =
    AzureAdTokenClientBuilder.builder()
        .withClientId(konfig.clientId)
        .withPrivateJwk(konfig.privateJwk)
        .withTokenEndpointUrl(konfig.tokenEndpointUrl)
        .withCache(CaffeineTokenCache())
        .buildMachineToMachineTokenClient()

private fun getAccessToken(
    pdlKlientKonfig: PdlKlientKonfigurasjon,
    autKonfig: AzureTokenKlientKonfigurasjon
): String {
    return aadMachineToMachineTokenClient(autKonfig).createMachineToMachineToken(
        "api://${pdlKlientKonfig.pdlCluster}.${pdlKlientKonfig.namespace}.${pdlKlientKonfig.appName}/.default"
    )
}