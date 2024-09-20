package no.nav.paw.kafkakeygenerator.client

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient

fun createKafkaKeyGeneratorClient(m2mTokenClient: AzureAdMachineToMachineTokenClient? = null): KafkaKeysClient {
    val kafkaKeyConfig = loadNaisOrLocalConfiguration<KafkaKeyConfig>("kafka_key_generator_client_config.toml")
    val m2mTC = m2mTokenClient ?: azureAdM2MTokenClient(
        currentRuntimeEnvironment,
        loadNaisOrLocalConfiguration<AzureM2MConfig>("azure_m2m.toml")
    )
    return kafkaKeysClient(kafkaKeyConfig) {
        m2mTC.createMachineToMachineToken(kafkaKeyConfig.scope)
    }
}
fun kafkaKeysClient(konfigurasjon: KafkaKeyConfig, m2mTokenFactory: () -> String): KafkaKeysClient =
    when (konfigurasjon.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysMedHttpClient(konfigurasjon, m2mTokenFactory)
    }

private fun kafkaKeysMedHttpClient(config: KafkaKeyConfig, m2mTokenFactory: () -> String): KafkaKeysClient {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }
    return StandardKafkaKeysClient(
        httpClient,
        config.url
    ) { m2mTokenFactory() }
}
