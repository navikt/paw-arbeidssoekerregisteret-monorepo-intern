package no.nav.paw.bekreftelsetjeneste.config

import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.client.KafkaKeyConfig
import java.net.InetAddress
import java.time.Duration
import java.time.Instant

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    val kafkaTopology: KafkaTopologyConfig,
    val kafkaStreams: KafkaConfig,
    val azureM2M: AzureM2MConfig,
    val kafkaKeysClient: KafkaKeyConfig,
    // Env
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    val hostname: String = InetAddress.getLocalHost().hostName
)

data class BekreftelseKonfigurasjon(
    val migreringstidspunkt: Instant,
    val interval: Duration,
    val graceperiode: Duration,
    val tilgjengeligOffset: Duration,
    val varselFoerGraceperiodeUtloept: Duration = graceperiode.dividedBy(2)
)

data class KafkaTopologyConfig(
    val applicationIdSuffix: String,
    val internStateStoreName: String,
    val ansvarStateStoreName: String,
    val periodeTopic: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelseloggTopic: String,
    val ansvarsTopic: String,
    val punctuationInterval: Duration,
    val shutdownTimeout: Duration = Duration.ofMinutes(5),
)
