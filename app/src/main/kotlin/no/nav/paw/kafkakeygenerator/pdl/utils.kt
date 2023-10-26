package no.nav.paw.kafkakeygenerator.pdl

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.paw.kafkakeygenerator.config.PdlKlientKonfigurasjon
import no.nav.paw.kafkakeygenerator.config.lastKonfigurasjon
import no.nav.paw.pdl.PdlClient


fun opprettKtorKlient() = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        jackson()
    }
}

fun opprettPdlKlient() =
    with(lastKonfigurasjon<PdlKlientKonfigurasjon>("pdl_klient")) {
        PdlClient(
            url = url,
            tema = tema,
            httpClient = opprettKtorKlient(),
            ::getAccessToken
        )
    }

private val aadMachineToMachineTokenClient = AzureAdTokenClientBuilder.builder()
    .withNaisDefaults()
    .buildMachineToMachineTokenClient()

private fun PdlKlientKonfigurasjon.getAccessToken(): String {
    return aadMachineToMachineTokenClient.createMachineToMachineToken(
        "api://${pdlCluster}.$namespace.$appName/.default"
    )
}