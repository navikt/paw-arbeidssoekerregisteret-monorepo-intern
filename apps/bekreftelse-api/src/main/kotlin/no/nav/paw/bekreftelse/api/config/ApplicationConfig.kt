package no.nav.paw.bekreftelse.api.config

import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.time.Duration

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val autorisasjon: AutorisasjonConfig,
    val authProviders: List<AuthProvider>,
    val azureM2M: AzureM2MConfig,
    val poaoClientConfig: ServiceClientConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    val kafkaClients: KafkaConfig,
    val kafkaTopology: KafkaTopologyConfig,
    val database: DatabaseConfig
)

data class AutorisasjonConfig(
    val corsAllowOrigins: String? = null
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

data class AuthProviderClaims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)

data class KafkaTopologyConfig(
    val version: Int,
    val antallPartitioner: Int,
    val producerId: String,
    val consumerId: String,
    val consumerGroupId: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelsesloggTopic: String
)

data class DatabaseConfig(
    val jdbcUrl: String,
    val driverClassName: String,
    val autoCommit: Boolean,
    val maxPoolSize: Int,
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    val idleTimeout: Duration = Duration.ofMinutes(10),
    val maxLifetime: Duration = Duration.ofMinutes(30)
)