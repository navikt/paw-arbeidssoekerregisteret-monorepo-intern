package no.nav.paw.meldeplikttjeneste

import java.time.Duration

data class ApplicationConfiguration(
    val periodeTopic: String,
    val ansvarsTopic: String,
    val rapporteringsTopic: String,
    val rapporteringsHendelsesloggTopic: String,
    val statStoreName: String,
    val punctuateInterval: Duration
)
