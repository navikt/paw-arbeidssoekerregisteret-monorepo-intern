package no.nav.paw.bekreftelsetjeneste.config

import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.net.InetAddress
import java.time.Duration

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val kafkaStreams: KafkaConfig,
    val azureM2M: AzureM2MConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    // Env
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    val hostname: String = InetAddress.getLocalHost().hostName
)

data class KafkaTopologyConfig(
    val applicationIdSuffix: String,
    val internStateStoreName: String,
    val periodeTopic: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelsesloggTopic: String,
    val punctuationInterval: Duration,
    val bekreftelseInterval: Duration,
    val bekreftelseGraceperiode: Duration,
    val bekreftelseTilgjengeligOffset: Duration,
)
