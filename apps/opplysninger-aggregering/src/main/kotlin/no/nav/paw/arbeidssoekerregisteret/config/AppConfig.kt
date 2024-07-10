package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentAppName
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaConfig
import java.time.Duration

const val SERVER_LOGGER_NAME = "no.nav.paw.server"
const val APPLICATION_LOGGER_NAME = "no.nav.paw.application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"

data class AppConfig(
    val kafka: KafkaConfig,
    val kafkaStreams: KafkaStreamsConfig,
    val appName: String = currentAppName ?: "paw-arbeidssoeker-opplysninger-aggregering",
    val appId: String = currentAppId ?: "paw-arbeidssoeker-opplysninger-aggregering:LOCAL",
    val naisEnv: NaisEnv = currentNaisEnv
)

data class KafkaStreamsConfig(
    val shutDownTimeout: Duration,
    val opplysingerStreamIdSuffix: String,
    val opplysningerTopic: String,
)
