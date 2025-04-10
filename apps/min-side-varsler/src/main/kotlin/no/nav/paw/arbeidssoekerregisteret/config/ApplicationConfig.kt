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
    val manueltVarselEnabled: Boolean,
    val manueltVarselSchedulingDelay: Duration,
    val manueltVarselSchedulingInterval: Duration,
    val oppryddingEnabled: Boolean,
    val oppryddingSchedulingDelay: Duration,
    val oppryddingSchedulingInterval: Duration,
    val oppryddingEldreEnn: Duration,
    val kafkaShutdownTimeout: Duration = Duration.ofSeconds(5)
)
