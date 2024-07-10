package no.nav.paw.arbeidssoekerregisteret.properties

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentAppId
import no.nav.paw.config.env.currentAppName
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.kafka.KafkaConfig
import java.time.Duration

const val SERVER_LOGGER_NAME = "no.nav.paw.server"
const val APPLICATION_LOGGER_NAME = "no.nav.paw.application"
const val APPLICATION_CONFIG_FILE_NAME = "application_configuration.toml"

data class ApplicationProperties(
    val kafka: KafkaConfig,
    val kafkaStreams: KafkaStreamsProperties,
    val appName: String = currentAppName ?: "paw-arbeidssoeker-opplysninger-aggregering",
    val appId: String = currentAppId ?: "paw-arbeidssoeker-opplysninger-aggregering:LOCAL",
    val naisEnv: NaisEnv = currentNaisEnv
)

data class KafkaStreamsProperties(
    val shutDownTimeout: Duration,
    val opplysingerStreamIdSuffix: String,
    val opplysningerTopic: String,
    val opplysningerStore: String,
    val opplysningerPunctuatorSchedule: Duration,
    val opplysningerLagretTidsperiode: Duration,
)
