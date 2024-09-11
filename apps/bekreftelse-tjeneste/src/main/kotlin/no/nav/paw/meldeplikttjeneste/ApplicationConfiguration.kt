package no.nav.paw.meldeplikttjeneste

import java.time.Duration

data class ApplicationConfiguration(
    val periodeTopic: String,
    val ansvarsTopic: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelseloggTopic: String,
    val stateStoreName: String,
    val punctuateInterval: Duration
)
