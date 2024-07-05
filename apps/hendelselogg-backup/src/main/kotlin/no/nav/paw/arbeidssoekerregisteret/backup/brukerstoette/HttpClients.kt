package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.auth.currentNaisEnv
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient

fun initClients(azureConfig: AzureM2MConfig): Pair<KafkaKeysClient, OppslagApiClient> {
    val azureTokenClient = azureAdM2MTokenClient(currentNaisEnv, azureConfig)
    val kafkaKeysClient = createKafkaKeyGeneratorClient(azureTokenClient)
    val oppslagApiClient = oppslagsApiClient(azureTokenClient)
    return kafkaKeysClient to oppslagApiClient
}

fun oppslagsApiClient(
    azureM2M: AzureAdMachineToMachineTokenClient
): OppslagApiClient {
    val cfg = loadNaisOrLocalConfiguration<OppslagApiConfig>(OPPSLAG_API_CONFIG)
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerKotlinModule()
            }
        }
    }
    return OppslagApiClient(
        config = cfg,
        getAccessToken = { azureM2M.createMachineToMachineToken(cfg.scope) },
        httpClient = httpClient
    )
}