package no.nav.paw.bekreftelse.api.config

import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val autorisasjon: AutorisasjonConfig,
    val kafkaTopology: KafkaTopologyConfig,
    val authProviders: AuthProviders,
    val azureM2M: AzureM2MConfig,
    val poaoClientConfig: ServiceClientConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    val kafkaClients: KafkaConfig
)

data class AutorisasjonConfig(
    val corsAllowOrigins: String? = null
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
    val clientId: String,
    val claims: AuthProviderClaims
)

typealias AuthProviders = List<AuthProvider>

data class AuthProviderClaims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)