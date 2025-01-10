package no.nav.paw.bekreftelse.api.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val autorisasjon: AutorisasjonConfig,
    val kafkaTopology: KafkaTopologyConfig,
    val database: DatabaseConfig
)

data class AutorisasjonConfig(
    val corsAllowOrigins: String? = null
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
