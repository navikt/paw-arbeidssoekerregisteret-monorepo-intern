package no.nav.paw.kafkakeygenerator.factory

import io.ktor.client.HttpClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.client.factory.createAzureAdM2MTokenClient
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.DefaultKafkaKeyGeneratorClient
import no.nav.paw.kafkakeygenerator.client.KAFKA_KEY_GENERATOR_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.client.KafkaKeyGeneratorClient
import no.nav.paw.kafkakeygenerator.client.MockKafkaKeyGeneratorClient
import no.nav.paw.kafkakeygenerator.config.KafkaKeyGeneratorClientConfig

fun buildKafkaKeyGeneratorClient(
    clientConfig: KafkaKeyGeneratorClientConfig = loadConfig(),
    httpClient: HttpClient = createHttpClient(),
    tokenClient: AzureAdMachineToMachineTokenClient = createAzureAdM2MTokenClient()
): KafkaKeyGeneratorClient {
    with(clientConfig) {
        if (url.lowercase().startsWith("mock")) {
            return MockKafkaKeyGeneratorClient()
        } else {
            return DefaultKafkaKeyGeneratorClient(httpClient, url) {
                tokenClient.createMachineToMachineToken(scope)
            }
        }
    }
}

private fun loadConfig(): KafkaKeyGeneratorClientConfig =
    loadNaisOrLocalConfiguration<KafkaKeyGeneratorClientConfig>(KAFKA_KEY_GENERATOR_CLIENT_CONFIG)