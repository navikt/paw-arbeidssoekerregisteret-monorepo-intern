package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val shutdownTimeout: Duration = Duration.ofSeconds(5),
    val varselProducerId: String,
    val periodeStreamSuffix: String,
    val bekreftelseStreamSuffix: String,
    val varselHendelseStreamSuffix: String,
    val periodeTopic: String,
    val bekreftelseHendelseTopic: String,
    val tmsVarselTopic: String,
    val tmsVarselHendelseTopic: String,
    val manueltVarselSchedulingDelay: Duration,
    val manueltVarselSchedulingPeriode: Duration,
)
