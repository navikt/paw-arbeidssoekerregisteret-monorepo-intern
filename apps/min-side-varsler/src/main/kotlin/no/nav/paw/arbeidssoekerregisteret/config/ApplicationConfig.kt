package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val varselProducerId: String,
    val periodeStreamSuffix: String,
    val bekreftelseStreamSuffix: String,
    val varselHendelseStreamSuffix: String,
    val periodeTopic: String,
    val bekreftelseHendelseTopic: String,
    val tmsVarselTopic: String,
    val tmsVarselHendelseTopic: String,
    val kafkaShutdownTimeout: Duration = Duration.ofSeconds(5)
)
