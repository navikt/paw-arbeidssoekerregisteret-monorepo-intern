package no.nav.paw.bekreftelse.api.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentAppName
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.net.InetAddress

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val authProviders: AuthProviders,
    val azureM2M: AzureM2MConfig,
    val poaoClientConfig: ServiceClientConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    val kafkaClients: KafkaConfig,
    // Env
    val naisEnv: NaisEnv = currentNaisEnv,
    val appId: String = currentAppId ?: "UNSPECIFIED",
    val appName: String = currentAppName ?: "UNSPECIFIED",
    val hostname: String = InetAddress.getLocalHost().hostName,

    val brukMock: Boolean = true
)

data class KafkaTopologyConfig(
    val applicationIdSuffix: String,
    val producerId: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelsesloggTopic: String,
    val internStateStoreName: String
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: Claims
)

typealias AuthProviders = List<AuthProvider>

data class Claims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)